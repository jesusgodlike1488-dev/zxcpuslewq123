package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {

    @Unique
    private float pulse$savedYaw;
    @Unique
    private float pulse$savedPitch;
    @Unique
    private boolean pulse$didSpoof = false;

    /**
     * tick HEAD:
     * 1. Сначала KillAura обновляет aimYaw через EventUpdate
     * 2. Потом ставим yaw = aimYaw НА ВЕСЬ ТИК
     * <p>
     * Дальше в этом тике:
     * super.tick() → tickMovement() → travel() использует getYaw() = aimYaw
     * → игрок двигается по aimYaw
     * → sendMovementPackets отправляет aimYaw + позицию
     * → сервер считает: yaw + движение совпадают → чисто
     * <p>
     * Раньше travel() использовал yaw камеры, а сервер получал aimYaw → не совпадало → флаг.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        try {
            PulseClient.getInstance().getEventBus().post(new EventUpdate());
        } catch (Exception ignored) {
        }

        pulse$didSpoof = false;

        if (RotationUtil.active) {
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;

            // Сохраняем камеру мыши
            pulse$savedYaw = self.getYaw();
            pulse$savedPitch = self.getPitch();

            // Ставим серверный yaw на ВЕСЬ тик (travel + sendMovementPackets)
            self.setYaw(RotationUtil.aimYaw);
            self.setPitch(RotationUtil.aimPitch);

            pulse$didSpoof = true;
        }
    }

    /**
     * sendMovementPackets TAIL:
     * Пакет движения ушёл (с правильными ротациями + правильной позицией).
     * Теперь бьём и возвращаем камеру.
     */
    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void postSendMovementPackets(CallbackInfo ci) {
        if (!pulse$didSpoof) return;

        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();

        // === АТАКА ===
        if (RotationUtil.attackThisTick && RotationUtil.target != null) {
            boolean wasSprinting = self.isSprinting();

            // 1. Легитно снимаем спринт ДО удара
            if (RotationUtil.shouldUnsprint && wasSprinting) {
                mc.getNetworkHandler().sendPacket(
                        new ClientCommandC2SPacket(self, ClientCommandC2SPacket.Mode.STOP_SPRINTING)
                );
                self.setSprinting(false);
            }

            // 2. Бьем цель
            mc.interactionManager.attackEntity(self, RotationUtil.target);

            // 3. Взмах рукой
            self.swingHand(Hand.MAIN_HAND);

            // 4. ВАЖНО: Сбрасываем ванильный кулдаун, чтобы не было рассинхрона с сервером!
            self.resetLastAttackedTicks();

            // ВНИМАНИЕ: Мы БОЛЬШЕ НЕ ОТПРАВЛЯЕМ пакет START_SPRINTING в этом же тике!
            // Ванильный клиент сам (в методе tickMovement) увидит, что кнопка W зажата,
            // и в следующем тике легитно отправит пакет начала спринта. Это обходит античит.

            RotationUtil.attackThisTick = false;
            RotationUtil.shouldUnsprint = false;
        }

        // === ВОССТАНОВЛЕНИЕ КАМЕРЫ ===
        String mode = RotationUtil.focusMode;

        if (mode.equalsIgnoreCase("Focus")) {
            self.setYaw(RotationUtil.aimYaw);
            self.setPitch(RotationUtil.aimPitch);
        } else if (mode.equalsIgnoreCase("Semi")) {
            float newYaw = pulse$savedYaw + MathHelper.wrapDegrees(RotationUtil.aimYaw - pulse$savedYaw) * 0.3f;
            float newPitch = pulse$savedPitch + (RotationUtil.aimPitch - pulse$savedPitch) * 0.3f;
            self.setYaw(newYaw);
            self.setPitch(newPitch);
        } else {
            self.setYaw(pulse$savedYaw);
            self.setPitch(pulse$savedPitch);
        }

        pulse$didSpoof = false;
    }
}
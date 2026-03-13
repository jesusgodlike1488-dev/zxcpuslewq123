package com.pulse.client.module.modules.combat;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import com.pulse.client.util.RotationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class KillAura extends Module {

    // Кулдаун по умолчанию 0.90 для идеальных критов в прыжке
    public final Setting<Double> reach = register(new Setting<>("Reach", 2.95, "Дальность удара").setRange(2.5, 3.0));
    public final Setting<Double> rotSpeed = register(new Setting<>("RotSpeed", 45.0, "Скорость наведения").setRange(10.0, 150.0));
    public final Setting<Boolean> smartCrit = register(new Setting<>("SmartCrit", true, "Криты (Ждать падения)"));
    public final Setting<Double> cooldownThreshold = register(new Setting<>("Cooldown", 0.90, "Кулдаун").setRange(0.85, 1.0));
    public final Setting<Boolean> wallCheck = register(new Setting<>("WallCheck", true, "Не бить сквозь стены"));
    public final Setting<String> focusMode = register(new Setting<>("FocusMode", "Free", "Free/Semi/Focus"));

    public static Entity currentTarget = null;
    private Entity target;

    // Переменные ротаций
    private float aimYaw, aimPitch;
    private float lastServerYaw, lastServerPitch;

    // Переменные таймингов и прыжков
    private boolean wasInAir = false;
    private long landTime = 0;
    private float randomCooldownOffset = 0.0f;

    public KillAura() {
        super("KillAura", "KillAura для FunTime", Category.COMBAT);
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        target = findTarget();
        currentTarget = target;

        // Если цели нет, плавно оставляем камеру на месте и чистим ротации
        if (target == null) {
            aimYaw = mc.player.getYaw();
            aimPitch = mc.player.getPitch();
            lastServerYaw = aimYaw;
            lastServerPitch = aimPitch;
            RotationUtil.reset();
            return;
        }

        // 1. Считаем идеальные углы с плавным покачиванием (защита от ML-AimBot)
        float[] exactRots = calculateIdealRotations(target);

        // 2. Доводим камеру с эффектом человеческого замедления
        smoothAim(exactRots[0], exactRots[1]);

        // 3. GCD Фикс (симуляция ванильной чувствительности мыши для GrimAC/Polar)
        float sens = mc.options.getMouseSensitivity().getValue().floatValue();
        float f = sens * 0.6F + 0.2F;
        float gcd = f * f * f * 8.0F * 0.15F;

        int deltaX = (int) ((aimYaw - lastServerYaw) / gcd);
        int deltaY = (int) ((aimPitch - lastServerPitch) / gcd);

        aimYaw = lastServerYaw + (deltaX * gcd);
        aimPitch = MathHelper.clamp(lastServerPitch + (deltaY * gcd), -90f, 90f);

        lastServerYaw = aimYaw;
        lastServerPitch = aimPitch;

        // 4. Передаем углы в MixinClientPlayerEntity (удары пока запрещены)
        RotationUtil.active = true;
        RotationUtil.aimYaw = aimYaw;
        RotationUtil.aimPitch = aimPitch;
        RotationUtil.target = this.target;
        RotationUtil.focusMode = this.focusMode.getValue();
        RotationUtil.attackThisTick = false;
        RotationUtil.shouldUnsprint = false;

        // 5. Проверка кулдауна с микро-рандомом (±0.015) для легитных критов и обхода AutoClicker
        float currentThreshold = MathHelper.clamp(cooldownThreshold.getValue().floatValue() + randomCooldownOffset, 0.85f, 1.0f);
        if (mc.player.getAttackCooldownProgress(0.5f) < currentThreshold) return;

        // 6. Raycast проверка (Reach/Angle фикс - бьем, только если физически смотрим на цель)
        if (!isLookingAtTarget(aimYaw, aimPitch, target, reach.getValue())) return;

        // 7. Smart Crit (не бьем в начале прыжка, чтобы не срывать крит)
        if (smartCrit.getValue()) {
            boolean inAir = !mc.player.isOnGround();
            boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava();
            boolean climbing = mc.player.isClimbing();
            boolean hasBlindness = mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);

            if (!inLiquid && !climbing && !hasBlindness) {
                if (inAir) {
                    wasInAir = true;
                    // Ожидаем начала реального падения вниз (погрешность 0.08f)
                    if (mc.player.fallDistance <= 0.08f || mc.player.getVelocity().y >= 0) return;
                } else {
                    if (wasInAir) {
                        wasInAir = false;
                        landTime = System.currentTimeMillis();
                        return; // Ждем 1 тик после приземления
                    }
                    if ((System.currentTimeMillis() - landTime) < 150) return;
                }
            }
        }

        // 8. Все проверки пройдены: разрешаем миксину нанести удар в этом тике
        RotationUtil.attackThisTick = true;
        RotationUtil.shouldUnsprint = true;

        // Генерируем новый рандомный оффсет для СЛЕДУЮЩЕГО удара (-0.015 до +0.015)
        randomCooldownOffset = (float) ((Math.random() - 0.5) * 0.03);
    }

    /**
     * Плавное наведение с симуляцией замедления руки (Lazy Aim)
     */
    private void smoothAim(float tgtYaw, float tgtPitch) {
        float yawDelta = MathHelper.wrapDegrees(tgtYaw - aimYaw);
        float pitchDelta = tgtPitch - aimPitch;

        float speed = rotSpeed.getValue().floatValue();

        // Если серверный луч УЖЕ на враге — почти не двигаем камеру (имитация фокуса)
        if (isLookingAtTarget(aimYaw, aimPitch, target, reach.getValue() + 0.5)) {
            speed *= 0.15f;
        }

        // Замедление наводки по мере приближения к центру
        float distanceToTarget = Math.abs(yawDelta) + Math.abs(pitchDelta);
        float multiplier = distanceToTarget < 25.0f ? (distanceToTarget / 25.0f) : 1.0f;
        multiplier = Math.max(multiplier, 0.15f);

        // Добавляем минимальное дрожание руки в движении
        float stepX = (speed * multiplier) + (float) (Math.random() * 2.0 - 1.0);
        float stepY = (speed * 0.7f * multiplier) + (float) (Math.random() * 2.0 - 1.0);

        aimYaw += MathHelper.clamp(yawDelta, -stepX, stepX);
        aimPitch += MathHelper.clamp(pitchDelta, -stepY, stepY);
    }

    /**
     * Идеальные ротации с плавающей синусоидой (Имитация дыхания/погрешности)
     */
    private float[] calculateIdealRotations(Entity entity) {
        Vec3d eyes = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        long time = System.currentTimeMillis();

        // Синусоида: плавно качает прицел (цикл 1.5 - 2 секунды)
        double swayX = Math.sin(time / 400.0) * 0.1;
        double swayY = Math.cos(time / 500.0) * 0.1;
        double swayZ = Math.sin(time / 450.0) * 0.1;

        // Целимся в 70% высоты (грудь/шея) + плавный сдвиг
        double targetX = box.getCenter().x + swayX;
        double targetY = box.minY + (box.maxY - box.minY) * 0.7 + swayY;
        double targetZ = box.getCenter().z + swayZ;

        double diffX = targetX - eyes.x;
        double diffY = targetY - eyes.y;
        double diffZ = targetZ - eyes.z;
        double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distXZ));

        return new float[]{yaw, pitch};
    }

    private boolean isLookingAtTarget(float yaw, float pitch, Entity target, double reach) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d lookVec = getVectorForRotation(pitch, yaw);
        Vec3d endPos = eyes.add(lookVec.multiply(reach));
        Box box = target.getBoundingBox().expand(0.1);
        return box.raycast(eyes, endPos).isPresent();
    }

    private Vec3d getVectorForRotation(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180F);
        float g = -yaw * ((float) Math.PI / 180F);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d((double) (i * j), (double) (-k), (double) (h * j));
    }

    private boolean canSeeTarget(Entity target) {
        if (!wallCheck.getValue()) return true;
        Vec3d eyes = mc.player.getEyePos();
        Box box = target.getBoundingBox();

        // 3 точки для проверки: центр, голова, ноги
        Vec3d[] points = {
                box.getCenter(),
                new Vec3d(box.getCenter().x, box.maxY - 0.1, box.getCenter().z),
                new Vec3d(box.getCenter().x, box.minY + 0.1, box.getCenter().z)
        };

        for (Vec3d point : points) {
            RaycastContext ctx = new RaycastContext(eyes, point,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            if (mc.world.raycast(ctx).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Поиск ближайшего легитного игрока через обычный цикл (чтобы не было ошибок Iterable в Fabric 1.21)
     */
    private Entity findTarget() {
        Entity best = null;
        double bestDist = reach.getValue() + 0.5; // Ищем чуть дальше, чтобы плавно наводиться заранее

        for (Entity en : mc.world.getEntities()) {
            if (en == mc.player || !(en instanceof PlayerEntity) || !en.isAlive()) continue;

            double dist = mc.player.distanceTo(en);
            if (dist > bestDist) continue;
            if (!canSeeTarget(en)) continue;

            bestDist = dist;
            best = en;
        }
        return best;
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            aimYaw = mc.player.getYaw();
            aimPitch = mc.player.getPitch();
            lastServerYaw = aimYaw;
            lastServerPitch = aimPitch;
        }
        target = null;
        currentTarget = null;
        wasInAir = false;
        landTime = 0;
        randomCooldownOffset = 0.0f;
        RotationUtil.reset();
    }

    @Override
    public void onDisable() {
        // Защита от телепортации камеры при выключении (Snap Bypass)
        if (mc.player != null) {
            if (target != null || RotationUtil.active) {
                mc.player.setYaw(aimYaw);
                mc.player.setPitch(aimPitch);
            }
        }

        target = null;
        currentTarget = null;
        wasInAir = false;
        landTime = 0;
        RotationUtil.reset();
    }
}
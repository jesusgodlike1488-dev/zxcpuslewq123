package com.pulse.client.gui;

import com.pulse.client.PulseClient;
import com.pulse.client.account.AccountManager;
import com.pulse.client.account.OfflineAccount;
import com.pulse.client.gui.font.FontManager;
import com.pulse.client.util.BlurUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Account Manager screen – add, remove and log in to offline accounts.
 */
public class AccountManagerScreen extends Screen {

    private final Screen parent;
    private final AccountManager am;

    // Add-account input
    private String  inputText  = "";
    private boolean inputFocus = true;
    private long    cursorBlink;
    private String  statusMsg  = "";
    private int     statusTimer = 0;

    // List state
    private int hoveredIdx = -1;
    private static final int ITEM_H  = 22;
    private static final int LIST_W  = 260;

    public AccountManagerScreen(Screen parent) {
        super(Text.literal("Account Manager"));
        this.parent = parent;
        this.am     = PulseClient.getInstance().getAccountManager();
    }

    // ─────────────────────────────── render ─────────────────────────────── //

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Background
        ctx.fill(0, 0, width, height, 0xCC000000);
        BlurUtil.drawGradientRect(ctx, 0, 0, width, height / 3, 0x441E3A6E, 0x00000000);

        int panelW = LIST_W + 40;
        int panelX = (width  - panelW) / 2;
        int panelY = 40;

        // Title
        FontManager.TITLE.drawCenteredStringWithShadow(ctx, "Account Manager", width / 2f, panelY, 0xFF1E90FF);
        FontManager.SMALL.drawCenteredString(ctx, "Управление оффлайн аккаунтами", width / 2f, panelY + FontManager.TITLE.getHeight() + 3, 0xFF555555);

        int contentY = panelY + FontManager.TITLE.getHeight() + 24;

        // Add account field
        renderAddField(ctx, mx, my, panelX, contentY, panelW);
        contentY += 36;

        // Account list
        renderList(ctx, mx, my, panelX, contentY, panelW);

        // Status message
        if (statusTimer > 0) {
            statusTimer--;
            boolean ok = statusMsg.startsWith("✔");
            FontManager.REGULAR.drawCenteredString(ctx, statusMsg, width / 2f,
                height - 30, ok ? 0xFF44CC44 : 0xFFCC4444);
        }

        super.render(ctx, mx, my, delta);
    }
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // убираем dirt
    }

    private void renderAddField(DrawContext ctx, int mx, int my, int px, int py, int pw) {
        int fieldW = pw - 80;
        int fieldH = 22;
        int fieldX = px;
        int btnX   = fieldX + fieldW + 6;
        int btnW   = pw - fieldW - 6;

        // Input field
        boolean hovered = mx >= fieldX && mx <= fieldX + fieldW && my >= py && my <= py + fieldH;
        BlurUtil.drawRoundedRect(ctx, fieldX + 1, py + 1, fieldW, fieldH, 4f, 0x33000000);
        BlurUtil.drawRoundedRect(ctx, fieldX, py, fieldW, fieldH, 4f, 0xEE0E0E0E);
        int borderCol = inputFocus ? 0xFF1E90FF : (hovered ? 0xFF444444 : 0xFF2A2A2A);
        BlurUtil.drawRoundedRectOutline(ctx, fieldX, py, fieldW, fieldH, 4f, 1f, borderCol);

        String display = inputText;
        if (inputFocus) {
            long now = System.currentTimeMillis();
            if ((now - cursorBlink) / 500 % 2 == 0) display += "|";
        }
        String placeholder = inputText.isEmpty() && !inputFocus ? "Имя аккаунта..." : display;
        int textCol = inputText.isEmpty() && !inputFocus ? 0xFF444444 : 0xFFCCCCCC;
        FontManager.REGULAR.drawString(ctx, placeholder, fieldX + 7, py + (fieldH - FontManager.REGULAR.getHeight()) / 2f, textCol);

        // "Add" button
        boolean btnHov = mx >= btnX && mx <= btnX + btnW && my >= py && my <= py + fieldH;
        BlurUtil.drawRoundedRect(ctx, btnX + 1, py + 1, btnW, fieldH, 4f, 0x33000000);
        BlurUtil.drawRoundedRect(ctx, btnX, py, btnW, fieldH, 4f, btnHov ? 0xFF1E90FF : 0xFF1A1A2E);
        BlurUtil.drawRoundedRectOutline(ctx, btnX, py, btnW, fieldH, 4f, 1f, btnHov ? 0xFF4499FF : 0xFF1E3A6E);
        FontManager.REGULAR.drawCenteredString(ctx, "Добавить",
            btnX + btnW / 2f, py + (fieldH - FontManager.REGULAR.getHeight()) / 2f, 0xFFFFFFFF);
    }

    private void renderList(DrawContext ctx, int mx, int my, int px, int py, int pw) {
        List<OfflineAccount> accounts = am.getAccounts();

        if (accounts.isEmpty()) {
            FontManager.REGULAR.drawCenteredString(ctx, "Нет сохранённых аккаунтов", (px + pw / 2f), py + 20, 0xFF444444);
            return;
        }

        FontManager.SMALL.drawString(ctx, "Сохранённые аккаунты (" + accounts.size() + ")", px, py - 14, 0xFF888888);

        for (int i = 0; i < accounts.size(); i++) {
            OfflineAccount acc = accounts.get(i);
            float iy = py + i * (ITEM_H + 3);
            boolean hov = mx >= px && mx <= px + pw && my >= iy && my <= iy + ITEM_H;

            // Row background
            BlurUtil.drawRoundedRect(ctx, px + 1, iy + 1, pw, ITEM_H, 4f, 0x22000000);
            BlurUtil.drawRoundedRect(ctx, px, iy, pw, ITEM_H, 4f, hov ? 0xFF1A1F2E : 0xFF0E0E0E);
            BlurUtil.drawRoundedRectOutline(ctx, px, iy, pw, ITEM_H, 4f, 1f, hov ? 0xFF1E3A6E : 0xFF1C1C1C);

            // Avatar placeholder (colored square)
            int avatar = 0xFF1E90FF + (acc.username().hashCode() & 0x00AAAAAA);
            ctx.fill(px + 4, (int)iy + 4, px + 4 + ITEM_H - 8, (int)iy + ITEM_H - 4, avatar);

            // Username
            FontManager.REGULAR.drawString(ctx, acc.username(),
                px + ITEM_H - 2, iy + (ITEM_H - FontManager.REGULAR.getHeight()) / 2f, 0xFFEEEEEE);

            // UUID (truncated)
            String uuidShort = acc.uuid().toString().substring(0, 8) + "...";
            FontManager.SMALL.drawString(ctx, uuidShort,
                px + ITEM_H - 2, iy + (ITEM_H - FontManager.REGULAR.getHeight()) / 2f + FontManager.REGULAR.getHeight() + 1, 0xFF444444);

            // Login button
            int loginBtnW = 50;
            float loginX = px + pw - loginBtnW - 4;
            boolean loginHov = mx >= loginX && mx <= loginX + loginBtnW && my >= iy + 3 && my <= iy + ITEM_H - 3;
            BlurUtil.drawRoundedRect(ctx, loginX, iy + 3, loginBtnW, ITEM_H - 6, 3f,
                loginHov ? 0xFF1E90FF : 0xFF12213A);
            FontManager.SMALL.drawCenteredString(ctx, "Войти",
                loginX + loginBtnW / 2f, iy + 3 + ((ITEM_H - 6) - FontManager.SMALL.getHeight()) / 2f,
                loginHov ? 0xFFFFFFFF : 0xFF8899AA);

            // Remove button
            float removeX = loginX - 26;
            boolean removeHov = mx >= removeX && mx <= removeX + 22 && my >= iy + 3 && my <= iy + ITEM_H - 3;
            BlurUtil.drawRoundedRect(ctx, removeX, iy + 3, 22, ITEM_H - 6, 3f,
                removeHov ? 0xFF8B0000 : 0xFF1E0000);
            FontManager.SMALL.drawCenteredString(ctx, "✕",
                removeX + 11, iy + 3 + ((ITEM_H - 6) - FontManager.SMALL.getHeight()) / 2f,
                removeHov ? 0xFFFF4444 : 0xFF884444);
        }
    }

    // ─────────────────────────── events ─────────────────────────────────── //

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        List<OfflineAccount> accounts = am.getAccounts();

        int pw     = LIST_W + 40;
        int panelX = (width  - pw) / 2;
        int panelY = 40;
        int contentY = panelY + FontManager.TITLE.getHeight() + 24;

        // Add field area
        int fieldH = 22;
        int fieldW = pw - 80;
        int btnX   = panelX + fieldW + 6;
        int btnW   = pw - fieldW - 6;

        if (mx >= panelX && mx <= panelX + fieldW && my >= contentY && my <= contentY + fieldH) {
            inputFocus  = true;
            cursorBlink = System.currentTimeMillis();
            return true;
        }

        // "Add" button
        if (mx >= btnX && mx <= btnX + btnW && my >= contentY && my <= contentY + fieldH) {
            if (!inputText.isBlank()) {
                boolean ok = am.addAccount(inputText.trim());
                statusMsg   = ok ? "✔ Добавлен: " + inputText.trim() : "✖ Уже существует или неверное имя";
                statusTimer = 80;
                if (ok) inputText = "";
            }
            return true;
        }

        inputFocus = false;
        contentY += 36;

        // List items
        for (int i = 0; i < accounts.size(); i++) {
            float iy     = contentY + i * (ITEM_H + 3);
            float loginX = panelX + pw - 54;
            float removeX = loginX - 26;

            // Login
            if (mx >= loginX && mx <= loginX + 50 && my >= iy + 3 && my <= iy + ITEM_H - 3) {
                boolean ok = am.login(accounts.get(i));
                statusMsg   = ok ? "✔ Вошли как " + accounts.get(i).username() : "✖ Ошибка смены аккаунта";
                statusTimer = 80;
                return true;
            }
            // Remove
            if (mx >= removeX && mx <= removeX + 22 && my >= iy + 3 && my <= iy + ITEM_H - 3) {
                am.removeAccount(i);
                statusMsg   = "✔ Аккаунт удалён";
                statusTimer = 60;
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputFocus) {
            if (keyCode == 259) { // Backspace
                if (!inputText.isEmpty()) inputText = inputText.substring(0, inputText.length() - 1);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter / NumpadEnter
                if (!inputText.isBlank()) {
                    boolean ok = am.addAccount(inputText.trim());
                    statusMsg   = ok ? "✔ Добавлен: " + inputText.trim() : "✖ Уже существует";
                    statusTimer = 80;
                    if (ok) inputText = "";
                }
                return true;
            }
            if (keyCode == 256) { inputFocus = false; return true; }
        }
        if (keyCode == 256) { this.client.setScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (inputFocus && chr >= 32 && inputText.length() < 16 && (Character.isLetterOrDigit(chr) || chr == '_')) {
            inputText += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}

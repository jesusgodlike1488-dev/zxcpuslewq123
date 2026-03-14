package com.pulse.client.gui.font;

import java.awt.Font;

public class FontManager {

    public static AWTFontRenderer TITLE;
    public static AWTFontRenderer REGULAR;
    public static AWTFontRenderer SMALL;
    public static AWTFontRenderer ICONS; // НОВЫЙ РЕНДЕРЕР ДЛЯ ИКОНОК

    // Стандартные символы для текста
    private static final String TEXT_CHARS =
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
                    "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "0123456789!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?`~ ";

    // Юникод-символы иконок из FontAwesome
    // \uf013=gear \uf007=user \uf05b=crosshairs \uf11b=gamepad \uf0c9=bars
    // \uf002=search \uf06e=eye \uf0ad=wrench \uf015=home \uf0a0=hdd
    private static final String ICON_CHARS = "\uf013\uf007\uf05b\uf11b\uf0c9\uf002\uf06e\uf0ad\uf015\uf0a0 ";

    public static void init() {
        TITLE   = loadFont("/assets/pulseclient/font/nunito_semibold.ttf", 26f, TEXT_CHARS);
        REGULAR = loadFont("/assets/pulseclient/font/nunito_semibold.ttf", 13f, TEXT_CHARS);
        SMALL   = loadFont("/assets/pulseclient/font/nunito_semibold.ttf", 10f, TEXT_CHARS);

        // ЗАГРУЖАЕМ ИКОНКИ
        ICONS   = loadFont("/assets/pulseclient/font/fa-solid-900.ttf", 11f, ICON_CHARS);
    }

    private static AWTFontRenderer loadFont(String path, float size, String chars) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, FontManager.class.getResourceAsStream(path))
                    .deriveFont(Font.PLAIN, size);
            // Теперь мы передаем символы в рендерер!
            return new AWTFontRenderer(font, chars);
        } catch (Exception e) {
            e.printStackTrace();
            return new AWTFontRenderer(new Font("Arial", Font.PLAIN, (int) size), chars);
        }
    }
}
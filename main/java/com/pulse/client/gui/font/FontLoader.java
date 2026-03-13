package com.pulse.client.gui.font;

import java.awt.Font;
import java.io.InputStream;

public class FontLoader {

    // Сюда мы добавили все буквы, цифры и нужные нам значки (в конце строки)
    private static final String CHARS =
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
                    "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "0123456789" +
                    "!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?`~ " +
                    "⚙✓✕▸▾➤◄►★⚠≡+>"; // <-- Иконки интерфейса здесь!

    public static AWTFontRenderer loadNunitoFont() {
        try {
            // Путь к шрифту внутри src/main/resources
            InputStream fontStream = FontLoader.class.getResourceAsStream("/assets/pulseclient/font/nunito_semibold.ttf");
            if (fontStream == null) {
                throw new RuntimeException("Font file not found in resources!");
            }

            // Загружаем шрифт и задаём размер 24
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(24f);

            // Создаём свой рендерер с этим шрифтом и передаем список символов CHARS
            return new AWTFontRenderer(font, CHARS);

        } catch (Exception e) {
            e.printStackTrace();

            // Если что-то пошло не так, возвращаем дефолтный шрифт Arial
            Font fallback = new Font("Arial", Font.PLAIN, 24);
            return new AWTFontRenderer(fallback, CHARS); // Здесь тоже передаем CHARS!
        }
    }
}
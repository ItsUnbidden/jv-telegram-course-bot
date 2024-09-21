package com.unbidden.telegramcoursesbot.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
public class KeyboardUtil {
    public List<KeyboardRow> getKeyboard(List<KeyboardButton> buttons) {
        return null;
    }

    public List<List<InlineKeyboardButton>> getInlineKeyboard(
            List<InlineKeyboardButton> buttons) {
        final int amountOfRows = (int)Math.ceil(buttons.size() / 2.0);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        int counter = -1;

        for (int i = 0; i < amountOfRows; i++) {
            rows.add(new ArrayList<>());
        }
        for (int i = 0; i < buttons.size(); i++) {
            if (i % 2 == 0) {
                counter++;
            }
            rows.get(counter).add(buttons.get(i));
        }
        return rows;
    }
}

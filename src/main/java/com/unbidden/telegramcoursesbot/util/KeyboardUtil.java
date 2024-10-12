package com.unbidden.telegramcoursesbot.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Component
public class KeyboardUtil {
    @NonNull
    public List<List<InlineKeyboardButton>> getInlineKeyboard(
            @NonNull List<InlineKeyboardButton> buttons, int rowSize) {
        final int amountOfRows = (int)Math.ceil(buttons.size()
                / (double)((rowSize != 0) ? rowSize : 1));
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        int counter = -1;

        for (int i = 0; i < amountOfRows; i++) {
            rows.add(new ArrayList<>());
        }
        for (int i = 0; i < buttons.size(); i++) {
            if (i % rowSize == 0) {
                counter++;
            }
            rows.get(counter).add(buttons.get(i));
        }
        return rows;
    }
}

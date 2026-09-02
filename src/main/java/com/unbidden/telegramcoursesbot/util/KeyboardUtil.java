package com.unbidden.telegramcoursesbot.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.LinkButton;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;

@Component
public class KeyboardUtil {
    public List<InlineKeyboardRow> getInlineKeyboard(List<InlineKeyboardButton> buttons, int rowSize) {
        final int amountOfRows = (int)Math.ceil(buttons.size()
                / (double)((rowSize != 0) ? rowSize : 1));
        final List<InlineKeyboardRow> rows = new ArrayList<>();
        int counter = -1;

        for (int i = 0; i < amountOfRows; i++) {
            rows.add(new InlineKeyboardRow());
        }
        for (int i = 0; i < buttons.size(); i++) {
            if (i % ((rowSize != 0) ? rowSize : 1) == 0) {
                counter++;
            }
            rows.get(counter).add(buttons.get(i));
        }
        return rows;
    }

    public InlineKeyboardMarkup getMarkup(Page page, List<MenuSnapshotButton> snapshotButtons, List<Button> buttons) {
        final List<InlineKeyboardButton> inlineButtons = new ArrayList<>();

        for (int i = 0; i < buttons.size(); ++i) {
            final Button button = buttons.get(i);
            final MenuSnapshotButton snapshotButton = snapshotButtons.get(i);

            inlineButtons.add((InlineKeyboardButton)InlineKeyboardButton.builder()
                    .callbackData(!button.getClass().equals(LinkButton.class) ? snapshotButton.getId().toString() : null)
                    .url(button.getClass().equals(LinkButton.class) ? ((LinkButton)button).getUrl() : null)
                    .text(button.getName())
                    .build());
        }
                
        return InlineKeyboardMarkup.builder()
                .keyboard(getInlineKeyboard(inlineButtons, page.getColumns()))
                .build();        
    }
}

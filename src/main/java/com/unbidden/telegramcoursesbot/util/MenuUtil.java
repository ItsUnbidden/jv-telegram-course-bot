package com.unbidden.telegramcoursesbot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import com.unbidden.telegramcoursesbot.exception.MenuParamsParseException;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.LinkButton;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;

@Component
public class MenuUtil {
    private static final String ELEMENT_DIVIDER = ":";

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

    public List<Integer> stringToIntList(String str) {
        final List<String> list = stringToList(str);

        return new ArrayList<>(list.stream().map(s -> Integer.parseInt(s)).toList());
    }

    public List<String> stringToList(String str) {
        final List<String> history = new ArrayList<>();

        if (str == null) return history;

        final String[] splitStr = str.split(ELEMENT_DIVIDER);

        for (final String part : splitStr) {
            history.add(part);
        }

        return history;
    }

    public String listToString(List<?> list) {
        if (list.isEmpty()) {
            return null;
        }

        final StringBuilder builder = new StringBuilder();

        for (final Object obj : list) {
            if (obj == null) {
                throw new MenuParamsParseException("List elements can't be null.");
            }
            final String str = obj.toString();

            if (str.contains(ELEMENT_DIVIDER)) {
                throw new MenuParamsParseException("The result of toString() of the objects in the list cannot contain \"" + ELEMENT_DIVIDER + "\".");
            }
            builder.append(str).append(ELEMENT_DIVIDER);
        }
        if (builder.length() > 0) {
            builder.delete(builder.length() - 1, builder.length());
        }
        
        return builder.toString();
    }

    public Map<String, String> stringToMap(String str) {
        final Map<String, String> map = new HashMap<>();

        if (str == null) return map;

        final String[] splitStr = str.split(ELEMENT_DIVIDER);

        for (int i = 0; i < splitStr.length; i += 2) {
            map.put(splitStr[i], splitStr[i + 1]);
        }

        return map;
    }

    public String mapToString(Map<String, String> params) {
        if (params.isEmpty()) {
            return null;
        }

        final StringBuilder builder = new StringBuilder();

        for (final Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new MenuParamsParseException("Menu parameter keys and values can't be null.");
            }
            if (entry.getKey().contains(ELEMENT_DIVIDER) || entry.getValue().contains(ELEMENT_DIVIDER)) {
                throw new MenuParamsParseException("Menu parameter keys and values can't contain \"" + ELEMENT_DIVIDER + "\".");
            }
            if (entry.getKey().isBlank() || entry.getValue().isBlank()) {
                throw new MenuParamsParseException("Menu parameter keys and values can't be blank.");
            }
            builder.append(entry.getKey()).append(ELEMENT_DIVIDER).append(entry.getValue()).append(ELEMENT_DIVIDER);
        }
        if (builder.length() > 0) {
            builder.delete(builder.length() - 1, builder.length());
        }
        
        return builder.toString();
    }
}

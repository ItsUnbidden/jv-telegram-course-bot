package com.unbidden.telegramcoursesbot.util;

import jakarta.annotation.PostConstruct;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
public class TextUtil {
    private static final Map<String, String> MARKERS = new HashMap<>();
    private static final String FIRST_NAME_PATTERN = "${firstName}";
    private static final String LAST_NAME_PATTERN = "${lastName}";
    private static final String USERNAME_PATTERN = "${username}";

    @PostConstruct
    public void init() {
        MARKERS.put("**", "bold");
        MARKERS.put("__", "italic");
        MARKERS.put("--", "underline");
        MARKERS.put("~~", "strikethrough");
        MARKERS.put("^^", "spoiler");
    }

    @NonNull
    public String injectUserData(@NonNull String text, @NonNull User user) {
        return text.replace(FIRST_NAME_PATTERN, user.getFirstName())
                .replace(LAST_NAME_PATTERN, user.getLastName())
                .replace(USERNAME_PATTERN, user.getUserName());
    }

    @NonNull
    public String injectParams(@NonNull String text, @NonNull Map<String, Object> params) {
        for (Entry<String, Object> entry : params.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue().toString());
        }
        return text;
    }

    @NonNull
    public Photo parsePhoto(@NonNull String rawFileStr) {
        final String[] data = rawFileStr.split("\n");

        if (data.length == 4) {
            Photo photo = new Photo();
            photo.setUrl(data[0]);
            photo.setSize(Integer.parseInt(data[1]));
            photo.setWidth(Integer.parseInt(data[2]));
            photo.setHeight(Integer.parseInt(data[3]));
            return photo;
        }
        throw new InvalidParameterException("Unable to parse photo in file.");
    }

    @NonNull
    public List<MessageEntity> getEntities(@NonNull String text) {
        final List<MessageEntity> entities = new ArrayList<>();

        for (Entry<String, String> entry : MARKERS.entrySet()) {
            int fromIndex = 0;
            int firstIndex = -1;
            int offset = 0;

            while (text.indexOf(entry.getKey(), fromIndex) > 0) {
                if (firstIndex == -1) {
                    firstIndex = text.indexOf(entry.getKey(), fromIndex);
                    fromIndex = firstIndex + 2;
                    continue;
                }
                int secondIndex = text.indexOf(entry.getKey(), fromIndex);
                entities.add(MessageEntity.builder()
                        .type(entry.getValue())
                        .offset(firstIndex - 4 * offset)
                        .length(secondIndex - firstIndex - 2)
                        .build());
                firstIndex = -1;
                fromIndex = secondIndex + 2;
                offset++;
            }
            text = text.replace(entry.getKey(), "");
        }
        return entities;
    }

    @NonNull
    public String removeMarkers(@NonNull String text) {
        for (Entry<String, String> entry : MARKERS.entrySet()) {
            text = text.replace(entry.getKey(), "");
        }
        return text;
    }
}

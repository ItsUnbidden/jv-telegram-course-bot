package com.unbidden.telegramcoursesbot.util;

import java.security.InvalidParameterException;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
public class TextUtil {
    private static final String FIRST_NAME_PATTERN = "${firstName}";
    private static final String LAST_NAME_PATTERN = "${lastName}";
    private static final String USERNAME_PATTERN = "${username}";

    @NonNull
    public String injectUserData(@NonNull String text, @NonNull User user) {
        String preparedStr = text.replace(FIRST_NAME_PATTERN, user.getFirstName())
                .replace(LAST_NAME_PATTERN, user.getLastName())
                .replace(USERNAME_PATTERN, user.getUserName());
        return preparedStr;
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
}

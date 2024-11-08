package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReceiveHomeworkToggleButtonHandler implements ButtonHandler {
    private static final String PARAM_STATUS = "${status}";

    private static final String SERVICE_TOGGLE_RECEIVE_HOMEWORK =
            "service_toggle_receive_homework";

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final UserEntity updatedUser = userService.toogleReceiveHomework(
                    userService.getUser(user.getId()));
            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_TOGGLE_RECEIVE_HOMEWORK, user, PARAM_STATUS,
                    (updatedUser.isReceivingHomeworkRequests()) ? "ENABLED" : "DISABLED");

            client.sendMessage(user, success);
        }
    }
}

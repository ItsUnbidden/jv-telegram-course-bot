package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeclineHomeworkButtonHandler implements ButtonHandler {
    private static final String SERVICE_DECLINE_HOMEWORK_COMMENT_REQUEST =
            "service_decline_homework_comment_request";

    private final HomeworkService homeworkService;

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Long homeworkProgressId = Long.parseLong(params[0]);
            sessionService.createSession(user, m ->
                    homeworkService.decline(homeworkProgressId, user, m));
    
            final Localization localization = localizationLoader.getLocalizationForUser(
                            SERVICE_DECLINE_HOMEWORK_COMMENT_REQUEST, user);
            client.sendMessage(user, localization);
        }
    }
}

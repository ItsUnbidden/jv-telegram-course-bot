package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
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
public class AcceptHomeworkButtonHandler implements ButtonHandler {
    private static final String ACCEPT_HOMEWORK_WITH_COMMENT_BUTTON = "ahwc";

    private static final String SERVICE_APPROVE_HOMEWORK_COMMENT_REQUEST =
            "service_approve_homework_comment_request";

    private final HomeworkService homeworkService;

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Long homeworkProgressId = Long.parseLong(params[0]);

            switch (params[params.length - 1]) {
                case ACCEPT_HOMEWORK_WITH_COMMENT_BUTTON:
                    sessionService.createSession(user, m -> homeworkService.approve(
                            homeworkProgressId, user, m));
                            
                    final Localization localization = localizationLoader.getLocalizationForUser(
                            SERVICE_APPROVE_HOMEWORK_COMMENT_REQUEST, user);
                    bot.sendMessage(user, localization);
                    break;
                default:
                    homeworkService.approve(homeworkProgressId, user, null);
                    break;
            }
        }
    }
}

package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetContentButtonHandler implements ButtonHandler {
    private static final String PARAM_CONTENT_ID = "${contentId}";
    
    private static final String SERVICE_GET_CONTENT_REQUEST = "service_get_content_request";
    private static final String SERVICE_GET_CONTENT_SUCCESS = "service_get_content_success";

    private static final String ERROR_PARSE_ID_FAILURE = "error_parse_id_failure";

    private final LocalizationLoader localizationLoader;

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final ContentService contentService;

    private final CustomTelegramClient client;
    
    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        final UserEntity userFromDb = userService.getUser(user.getId());
        if (!userService.isAdmin(userFromDb)) {
            return;
        }
        sessionService.createSession(user, m -> {
            final String providedNumberStr = m.get(0).getText().trim();
            final Long contentId;
            try {
                contentId = Long.parseLong(m.get(0).getText().trim());
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse provided string "
                        + providedNumberStr + " to content id long", localizationLoader
                        .getLocalizationForUser(ERROR_PARSE_ID_FAILURE, userFromDb), e);
            }
            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_GET_CONTENT_SUCCESS, user, PARAM_CONTENT_ID,
                    contentId);
            contentService.sendContent(contentService.getById(contentId, user), user);
            client.sendMessage(user, success);
        }, true);
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_GET_CONTENT_REQUEST, user);

        client.sendMessage(user, request);
    }
}

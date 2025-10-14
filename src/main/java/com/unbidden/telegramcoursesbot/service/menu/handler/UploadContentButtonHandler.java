package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UploadContentButtonHandler implements ButtonHandler {
    private static final String PARAM_CONTENT_ID = "${contentId}";
    
    private static final String SERVICE_UPLOAD_CONTENT_REQUEST = "service_upload_content_request";
    private static final String SERVICE_UPLOAD_CONTENT_SUCCESS = "service_upload_content_success";

    private final LocalizationLoader localizationLoader;

    private final ContentSessionService sessionService;

    private final ContentService contentService;

    private final ClientManager clientManager;
    
    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        sessionService.createSession(user, bot, m -> {
            final Content content = contentService.parseAndPersistContent(bot, m);
            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_UPLOAD_CONTENT_SUCCESS, user, PARAM_CONTENT_ID,
                    content.getId());
            clientManager.getClient(bot).sendMessage(user, success);
        });
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_UPLOAD_CONTENT_REQUEST, user);

        clientManager.getClient(bot).sendMessage(user, request);
    }
}

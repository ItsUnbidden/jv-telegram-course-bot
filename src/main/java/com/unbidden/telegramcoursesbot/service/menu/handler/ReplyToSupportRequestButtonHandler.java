package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.support.SupportService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplyToSupportRequestButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            SendSupportRequestButtonHandler.class);

    private static final String SERVICE_SUPPORT_REQUEST_REPLY_REQUEST =
            "service_support_request_reply_request";
    private static final String SERVICE_SUPPORT_REQUEST_REPLY_SENT =
            "service_support_request_reply_sent";

    private final ContentSessionService sessionService;
    
    private final ContentService contentService;

    private final SupportService supportService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.ANSWER_SUPPORT)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final SupportRequest request = supportService.getRequestById(
                Long.parseLong(params[0]), user, bot);
        LOGGER.info("User " + user.getId() + " is trying to reply to support request "
                + request.getId() + "...");

        supportService.checkRequestResolved(request, user, bot);
        supportService.checkSupportMessageAnswered(request, user, bot);

        sessionService.createSession(user, bot, m -> {
            final LocalizedContent content = contentService.parseAndPersistContent(bot, m);

            supportService.replyToSupportRequest(user, bot, request, content);
            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_SUPPORT_REQUEST_REPLY_SENT, user));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending support content request message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_SUPPORT_REQUEST_REPLY_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}

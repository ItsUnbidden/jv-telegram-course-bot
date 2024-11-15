package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
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
public class ReplyToSupportReplyButtonHandler implements ButtonHandler{
private static final Logger LOGGER = LogManager.getLogger(
            SendSupportRequestButtonHandler.class);

    private static final String SERVICE_SUPPORT_REPLY_REPLY_REQUEST =
            "service_support_reply_reply_request";
    private static final String SERVICE_SUPPORT_REPLY_REPLY_SENT =
            "service_support_reply_reply_sent";

    private final ContentSessionService sessionService;
    
    private final ContentService contentService;

    private final SupportService supportService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        final SupportReply reply = supportService.getReplyById(Long.parseLong(params[0]), user);
        LOGGER.info("User " + user.getId() + " is trying to reply to support reply "
                + reply.getId() + "...");

        supportService.checkRequestResolved(reply, user);
        supportService.checkSupportMessageAnswered(reply, user);
        
        sessionService.createSession(user, m -> {
            final LocalizedContent content = contentService.parseAndPersistContent(m);

            supportService.replyToReply(user, reply, content);
            LOGGER.debug("Sending confirmation message...");
            client.sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_SUPPORT_REPLY_REPLY_SENT, user));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending support content request message...");
        client.sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_SUPPORT_REPLY_REPLY_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}

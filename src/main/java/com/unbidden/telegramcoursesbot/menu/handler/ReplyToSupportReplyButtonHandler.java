package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.support.SupportService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplyToSupportReplyButtonHandler extends AbstractButtonHandler {
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

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.REPLY_SUPPORT)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final SupportReply reply = supportService.getSupportReplyById(Long.parseLong(params[0]),
                user, bot);
        LOGGER.info("User " + user.getId() + " is trying to reply to support reply "
                + reply.getId() + "...");

        supportService.checkRequestResolved(reply, user, bot);
        supportService.checkSupportMessageAnswered(reply, user, bot);
        
        sessionService.createSession(user, bot, m -> {
            final LocalizedContent content = contentService.parseAndPersistContent(bot, m);

            supportService.replyToReply(user, bot, reply, content);
            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .localize(SERVICE_SUPPORT_REPLY_REPLY_SENT, user));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending support content request message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                SERVICE_SUPPORT_REPLY_REPLY_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}

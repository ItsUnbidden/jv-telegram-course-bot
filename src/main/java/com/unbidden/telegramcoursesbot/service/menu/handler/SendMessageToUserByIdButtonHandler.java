package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.post.PostService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendMessageToUserByIdButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            SendMessageToUserByIdButtonHandler.class);

    private static final int EXPECTED_MESSAGES = 1;

    private static final String PARAM_USER_ID = "${userId}";
    private static final String PARAM_TARGET_FULL_NAME = "${targetFullName}";
    private static final String PARAM_TARGET_TITLE = "${targetTitle}";

    private static final String SERVICE_PRIVATE_MESSAGE_CONTENT_REQUEST =
            "service_private_message_content_request";
    private static final String SERVICE_PRIVATE_MESSAGE_USER_REQUEST =
            "service_private_message_user_request";
    private static final String SERVICE_PRIVATE_MESSAGE_SENT = "service_private_message_sent";

    private static final String ERROR_USER_ID_LESS_THAN_ZERO = "error_user_id_less_than_zero";
    private static final String ERROR_PARSE_USER_ID = "error_parse_user_id";

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final PostService postService;

    private final ContentService contentService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.POST)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        LOGGER.info("User " + user.getId() + " is trying to send a private message...");
        sessionService.createSession(user, bot, m -> {
            textUtil.checkExpectedMessages(EXPECTED_MESSAGES, user, m, localizationLoader);
            final long userId;
            try {
                userId = Long.parseLong(m.get(0).getText().trim());
                if (userId <= 0) {
                    throw new InvalidDataSentException("User id must be bigger than 0",
                            localizationLoader.getLocalizationForUser(
                            ERROR_USER_ID_LESS_THAN_ZERO, user,
                            PARAM_USER_ID, userId));
                }
                LOGGER.debug("User id has been parsed.");
                final UserEntity target = userService.getUser(userId, user);
                postService.checkUserIsInBot(user, target, bot);
                LOGGER.debug("User found. Asking for content.");
                requestContentAndSendMessage(user, target, bot);
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse provided string "
                        + m.get(0).getText().trim() + " to user id long",
                        localizationLoader.getLocalizationForUser(ERROR_PARSE_USER_ID,
                        user), e);
            }
        }, true);
        LOGGER.debug("Sending target user request message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_PRIVATE_MESSAGE_USER_REQUEST, user));
        LOGGER.debug("Request sent.");
    }

    private void requestContentAndSendMessage(UserEntity user, UserEntity target, Bot bot) {
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_TARGET_TITLE, userService.getLocalizedTitle(target, bot));
        parameterMap.put(PARAM_TARGET_FULL_NAME, target.getFullName());

        sessionService.createSession(user, bot, m -> {
            final LocalizedContent content = contentService.parseAndPersistContent(bot, m);

            LOGGER.info("Content " + content.getId() + " will be sent to user " + target.getId()
                    + " in bot " + bot.getName() + ".");
            postService.sendPrivateMessageToUser(user, target, bot, content);
            LOGGER.info("Private message to user " + target.getId() + " in bot "
                    + bot.getName() + " has been sent.");
            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_PRIVATE_MESSAGE_SENT, user, parameterMap));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending content request...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_PRIVATE_MESSAGE_CONTENT_REQUEST, user, parameterMap));
        LOGGER.debug("Request sent.");
    }
}

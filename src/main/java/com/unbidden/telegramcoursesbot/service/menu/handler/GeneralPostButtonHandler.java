package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.post.PostService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneralPostButtonHandler implements ButtonHandler {
private static final Logger LOGGER = LogManager.getLogger(PostButtonHandler.class);

    private static final String PARAM_MEDIA_TYPES = "${mediaTypes}";

    private static final String SERVICE_GENERAL_POST_STARTED = "service_general_post_started";
    private static final String SERVICE_GENERAL_POST_ROLES_REQUEST =
            "service_general_post_roles_request";
    private static final String SERVICE_GENERAL_POST_CONTENT_REQUEST =
            "service_general_post_content_request";

    private static final String ERROR_PARSE_ENUM_FAILURE = "error_parse_enum_failure";

    private static final int EXPECTED_MESSAGES = 1;
    private static final String POST_CUSTOM_ROLE_SET = "pcrs";
    private static final String ROLE_TYPES_DIVIDER = " ";

    private final PostService postService;

    private final ContentService contentService;

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final BotService botService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = {AuthorityType.MAINTENANCE, AuthorityType.POST})
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        botService.checkBotFather(bot, user);
        LOGGER.info("The director is trying to post...");
        postService.checkExecution(user);
        final List<Role> roles = new ArrayList<>();
        switch (params[0]) {
            case POST_CUSTOM_ROLE_SET:
                sessionService.createSession(user, bot, m -> {
                    textUtil.checkExpectedMessages(EXPECTED_MESSAGES, user,
                            m, localizationLoader);
                    final String[] potentialRoleTypes = m.get(0).getText().trim()
                            .split(ROLE_TYPES_DIVIDER);
                    for (String roleStr : potentialRoleTypes) {
                        try {
                            roles.add(userService.getRole(RoleType.valueOf(
                                    roleStr.toUpperCase())));
                        } catch (IllegalArgumentException e) {
                            throw new InvalidDataSentException("Unable to parse " + roleStr
                                    + " to a " + RoleType.class.getName() + " enum value",
                                    localizationLoader.getLocalizationForUser(
                                    ERROR_PARSE_ENUM_FAILURE, user, PARAM_MEDIA_TYPES,
                                    Arrays.toString(MediaType.values())), e);
                        }
                    }
                    LOGGER.debug("Roles parsed.");
                    post(user, bot, roles);
                });
                LOGGER.debug("Sending roles request...");
                clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                        .getLocalizationForUser(SERVICE_GENERAL_POST_ROLES_REQUEST, user));
                LOGGER.debug("Request sent.");
                break;
            default:
                roles.add(userService.getRole(RoleType.valueOf(params[0])));
                post(user, bot, roles);
                break;
        }
    }

    private void post(UserEntity user, Bot bot, List<Role> roles) {
        LOGGER.debug("Post will be sent to all users with one of the roles " + roles + "...");
        postService.checkExecution(user);
        sessionService.createSession(user, bot, m -> {
            final LocalizedContent postContent = contentService.parseAndPersistContent(bot, m);

            LOGGER.debug("Content with id " + postContent.getId() + " will be used for post.");
            postService.sendMessagesThroughoutBots(user, roles, postContent);

            LOGGER.debug("Sending thread started message...");
            clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_GENERAL_POST_STARTED, user));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending content request...");
        clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_GENERAL_POST_CONTENT_REQUEST, user));
        LOGGER.debug("Request sent.");
    }
}

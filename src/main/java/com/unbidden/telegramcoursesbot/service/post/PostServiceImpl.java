package com.unbidden.telegramcoursesbot.service.post;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private static final Logger LOGGER = LogManager.getLogger(PostServiceImpl.class);

    private static final String PARAM_MESSAGES_SENT = "${messagesSent}";
    private static final String PARAM_SENDER_FULL_NAME = "${senderFullName}";
    private static final String PARAM_TITLE = "${title}";

    private static final String SERVICE_POST_COMPLETED = "service_post_completed";
    private static final String SERVICE_GENERAL_POST_COMPLETED = "service_general_post_completed";
    private static final String SERVICE_PRIVATE_MESSAGE_INFO = "service_private_message_info";

    private static final String ERROR_PRIVATE_MESSAGE_USER_NOT_REGISTERED_IN_BOT =
            "error_private_message_user_not_registered_in_bot";
    private static final String ERROR_TOO_MANY_POST_REQUESTS = "error_too_many_post_requests";
    private static final String ERROR_POST_NO_ROLES = "error_post_no_roles";

    private static final int USERS_PER_REQUEST = 25;

    private volatile boolean isRequestBeingExecuted;

    private final BotRoleRepository botRoleRepository;

    private final ContentService contentService;

    private final BotService botService;

    private final ExecutorService executorService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    public void sendMessages(@NonNull UserEntity sender, @NonNull Bot bot,
            @NonNull List<Role> roles, @NonNull Content content) {
        checkExecution(sender);

        final Runnable runnable = () -> {
            LOGGER.info("Executing post request from user " + sender.getId() + " in bot "
                    + bot.getName() + "... Content id is " + content.getId() + ". Roles are "
                    + roles.stream().map(r -> r.getType()).toList() + ".");
            isRequestBeingExecuted = true;
            List<UserEntity> targets;
            int totalMessagesCounter = 0;
            for (Role role : roles) {
                LOGGER.debug("Sending content " + content.getId() + " to users with role type "
                        + role.getType() + " in bot " + bot.getName() + "...");
                int pageCounter = 0;
                int userCounter = 0;
                do {
                    targets = botRoleRepository.findByBotAndRoleType(bot,
                            role.getType(), PageRequest.of(pageCounter, USERS_PER_REQUEST))
                            .stream().map(br -> br.getUser()).toList();
                    pageCounter++;
                    for (UserEntity target : targets) {
                        contentService.sendContent(content, target, bot);
                        userCounter++;
                    }
                } while (targets.size() > 0);
                LOGGER.debug("Content " + content.getId() + " has been sent to "
                        + userCounter + " users.");
                totalMessagesCounter += userCounter;
            }
            LOGGER.info("Thread which was executing post request for user " + sender.getId()
                    + " in bot " + bot.getId() + ", has finished execution with "
                    + totalMessagesCounter + " messages sent.");
            isRequestBeingExecuted = false;
            LOGGER.debug("Sending success message...");
            clientManager.getClient(bot).sendMessage(sender, localizationLoader
                    .getLocalizationForUser(SERVICE_POST_COMPLETED, sender,
                    PARAM_MESSAGES_SENT, totalMessagesCounter));
            LOGGER.debug("Message sent.");
        };
        executorService.submit(runnable);
    }

    @Override
    public void sendMessagesThroughoutBots(@NonNull UserEntity director,
            @NonNull List<Role> roles, @NonNull Content content) {
        checkExecution(director);

        final Runnable runnable = () -> {
            LOGGER.info("Executing general post request for the director... Message will be "
                    + "sent to all users with roles " + roles.stream()
                    .map(r -> r.getType()).toList() + ". Content id is " + content.getId() + ".");
            isRequestBeingExecuted = true;
            List<UserEntity> targets;
            List<Bot> bots;
            int totalMessagesCounter = 0;
            for (Role role : roles) {
                LOGGER.debug("Sending content " + content.getId() + " to users with role type "
                        + role.getType() + "...");
                int pageCounter = 0;
                int userCounter = 0;
                do {
                    final List<BotRole> botRoles = botRoleRepository.findByRoleType(
                            role.getType(), PageRequest.of(pageCounter, USERS_PER_REQUEST));
                    targets = new ArrayList<>();
                    bots = new ArrayList<>();
                    for (BotRole botRole : botRoles) {
                        if (!botRole.getBot().getId().equals(botService.getBotFather().getId())) {
                            targets.add(botRole.getUser());
                            bots.add(botRole.getBot());
                        }
                    }
                    pageCounter++;
                    for (int i = 0; i < targets.size(); i++) {
                        contentService.sendContent(content, targets.get(i), bots.get(i));
                        userCounter++;
                    }
                } while (targets.size() > 0);
                LOGGER.debug("Content " + content.getId() + " has been sent to "
                        + userCounter + " users.");
                totalMessagesCounter += userCounter;
            }
            LOGGER.info("Thread which was executing general post request for director, "
                    + "has finished execution with " + totalMessagesCounter + " messages sent.");
            isRequestBeingExecuted = false;
            LOGGER.debug("Sending success message...");
            clientManager.getBotFatherClient().sendMessage(director, localizationLoader
                    .getLocalizationForUser(SERVICE_GENERAL_POST_COMPLETED, director,
                    PARAM_MESSAGES_SENT, totalMessagesCounter));
            LOGGER.debug("Message sent.");
        };
        executorService.submit(runnable);
    }

    @Override
    public void checkExecution(@NonNull UserEntity user) {
        if (isRequestBeingExecuted) {
            throw new ForbiddenOperationException("A request is already being executed. "
                    + "Only one is allowed at a time.", localizationLoader.getLocalizationForUser(
                    ERROR_TOO_MANY_POST_REQUESTS, user));
        }
    }

    @Override
    public void checkRoles(@NonNull List<Role> roles, @NonNull UserEntity user) {
        if (roles.size() == 0) {
            throw new ForbiddenOperationException("At least one role must be specified for post",
                    localizationLoader.getLocalizationForUser(ERROR_POST_NO_ROLES, user));
        }
    }

    @Override
    public void sendPrivateMessageToUser(@NonNull UserEntity user, @NonNull UserEntity target,
            @NonNull Bot bot, @NonNull Content content) {
        LOGGER.info("User " + user.getId() + " is sending a private message to user "
                + target.getId() + " in bot " + bot.getName() + "...");
        
        checkUserIsInBot(user, target, bot);
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_TITLE, userService.getLocalizedTitle(user, bot));
        parameterMap.put(PARAM_SENDER_FULL_NAME, user.getFullName());

        clientManager.getClient(bot).sendMessage(target, localizationLoader
                .getLocalizationForUser(SERVICE_PRIVATE_MESSAGE_INFO, user, parameterMap));
        LOGGER.debug("Info message sent.");
        contentService.sendContent(content, target, bot);
        LOGGER.debug("Content sent.");
    }

    @Override
    public void checkUserIsInBot(@NonNull UserEntity user, @NonNull UserEntity target, @NonNull Bot bot) {
        try {
            userService.getBotRole(target, bot);
        } catch (EntityNotFoundException e) {
            throw new ForbiddenOperationException("User " + target.getId()
                    + " is not registered in bot " + bot.getName(), localizationLoader
                    .getLocalizationForUser(ERROR_PRIVATE_MESSAGE_USER_NOT_REGISTERED_IN_BOT,
                    user));
        }
    }
}

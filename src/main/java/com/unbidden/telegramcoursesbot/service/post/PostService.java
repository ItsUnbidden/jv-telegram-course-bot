package com.unbidden.telegramcoursesbot.service.post;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;

import com.unbidden.telegramcoursesbot.util.EntityUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class PostService {
    private static final Logger LOGGER = LogManager.getLogger(PostService.class);

    private final BlockingQueue<Request> requestQueue = new LinkedBlockingDeque<>();

    private final ExecutorService postWorkerThreadExecutor;

    private final UserRepository userRepository;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final BotRoleRepository botRoleRepository;

    private final EntityUtil entityUtil;

    private Request currentRequest = null;

    @PostConstruct
    protected void start() {
        LOGGER.info("Starting post executor...");
        postWorkerThreadExecutor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    currentRequest = requestQueue.take();

                    LOGGER.info("Processing post request for user " + currentRequest.user.getId() + " and bot " + currentRequest.bot.getId() + "...");
                    try {
                        currentRequest.finalFuture.get();
                        
                        int successes = 0;
                        int failures = 0;
                        for (final var future : currentRequest.futures) {
                            if (future.isCompletedExceptionally()) {
                                ++failures;
                            } else {
                                ++successes;
                            }
                        }
                        LOGGER.info("Post request for user " + currentRequest.user.getId() + " and bot " + currentRequest.bot.getId()
                                + " has been completed. Sent messages: " + successes + ", failures: " + failures + ".");
                        clientManager.getClient(currentRequest.bot).sendMessage(currentRequest.user, localizationLoader
                                .localize(Localizations.Service.POST_COMPLETED, currentRequest.user,
                                    new Localizations.Service.PostCompletedParams(successes, failures))); // TODO: if exceptions are reintroduced for sendMessage(), this will become a problem
                    } catch (ExecutionException e) {
                        LOGGER.error("An error has occured while waiting for a post request to complete.", e);
                    }
                    currentRequest = null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public synchronized void sendMessages(UserEntity sender, Bot bot, List<Role> roles, List<Message> messages) {
        checkExecution(sender, bot);
        checkRoles(roles, sender);

        final LocalizedContent content = contentService.parseAndPersistContent(sender, bot, messages);

        LOGGER.info("Executing a post request from user " + sender.getId() + " in bot "
                + bot.getId() + "... Content id is " + content.getId() + ". Roles are "
                + roles.stream().map(r -> r.getType()).toList() + ".");
        final List<Long> userIds = userRepository.findAllIdsByBotIdAndRoleTypeIn(bot.getId(), roles.stream().map(r -> r.getType()).toList());
        final var futures = contentService.sendContentInBulkAsync(sender, bot, userIds, content);

        LOGGER.info("All of the requests have been initiated. Once they are all completed, a confirmation will be sent to "
                + sender.getId() + ".");

        final CompletableFuture<Void> finalFuture = CompletableFuture.allOf(futures.stream()
                .map(f -> f.handle((r, t) -> null))
                .toArray(CompletableFuture[]::new));

        requestQueue.add(new Request(sender, bot, futures, finalFuture));
    }

    private void checkExecution(UserEntity user, Bot bot) {
        if (currentRequest.bot.getId().equals(bot.getId()) || requestQueue.stream().anyMatch(r -> r.bot.getId().equals(bot.getId()))) {
            throw new ForbiddenOperationException("A request is already being executed. "
                    + "Only one is allowed per bot at a time.", localizationLoader.localize(
                    Error.TOO_MANY_POST_REQUESTS, user));
        }
    }

    private void checkRoles(List<Role> roles, UserEntity user) {
        if (roles.isEmpty()) {
            throw new ForbiddenOperationException("At least one role must be specified for post",
                    localizationLoader.localize(Error.POST_NO_ROLES, user));
        }
    }

    public void sendPrivateMessageToUser(UserEntity user, Bot bot, UserEntity target, List<Message> messages) {
        LOGGER.info("User " + user.getId() + " is sending a private message to user "
                + target.getId() + " in bot " + bot.getId() + "...");
        
        checkUserIsInBot(user, bot, target);

        clientManager.getClient(bot).sendMessage(target, localizationLoader
                .localize(Localizations.Service.PRIVATE_MESSAGE_INFO, target,
                    new Localizations.Service.PrivateMessageInfoParams(user.getFullName(),
                    entityUtil.getLocalizedTitle(target, bot, user))));
        LOGGER.debug("Info message sent.");
        contentService.sendContent(target, bot, contentService.parseAndPersistContent(user, bot, messages));
        LOGGER.debug("Content sent.");
    }

    @PreDestroy
    protected void stop() {
        postWorkerThreadExecutor.shutdownNow();
        LOGGER.info("Post executor has been ordered to shutdown.");
    }

    private void checkUserIsInBot(UserEntity user, Bot bot, UserEntity target) {
        if (!botRoleRepository.existsByBotIdAndUserId(bot.getId(), target.getId())) {
            throw new ForbiddenOperationException("User " + target.getId()
                    + " is not registered in bot " + bot.getId(), localizationLoader
                    .localize(Error.PRIVATE_MESSAGE_USER_NOT_REGISTERED_IN_BOT, user));
        }
    }

    private static class Request {
        UserEntity user;

        Bot bot;

        List<CompletableFuture<List<Message>>> futures;

        CompletableFuture<Void> finalFuture;

        Request(UserEntity user, Bot bot, List<CompletableFuture<List<Message>>> futures, CompletableFuture<Void> finalFuture) {
            this.user = user;
            this.bot = bot;  
            this.futures = futures;  
            this.finalFuture = finalFuture;  
        }
    }
}

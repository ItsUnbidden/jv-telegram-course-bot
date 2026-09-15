package com.unbidden.telegramcoursesbot.service.post;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;

import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class PostService {
    private static final Logger LOGGER = LogManager.getLogger(PostService.class);

    private static final int QUERY_PAGE_SIZE = 500;
    
    private volatile Request currentRequest = null;

    private final BlockingQueue<Request> requestQueue = new LinkedBlockingDeque<>();

    private final ExecutorService postWorkerThreadExecutor;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final BotRoleRepository botRoleRepository;

    private final EntityUtil entityUtil;

    private final ValidatorUtil validatorUtil;


    @PostConstruct
    protected void start() {
        LOGGER.info("Starting post executor...");
        postWorkerThreadExecutor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    currentRequest = requestQueue.take();

                    LOGGER.info("Processing post request for user " + currentRequest.botRole.getUser().getId()
                            + " and bot " + currentRequest.botRole.getBot().getId() + "...");
                    int successes = 0;
                    int failures = 0;
                    try {
                        currentRequest.finalFuture.get();
                        
                        for (final var future : currentRequest.futures) {
                            if (future.isCompletedExceptionally()) {
                                ++failures;
                            } else {
                                ++successes;
                            }
                        }
                        LOGGER.info("Post request for user " + currentRequest.botRole.getUser().getId() + " and bot " + currentRequest.botRole.getBot().getId()
                                + " has been completed. Sent messages: " + successes + ", failures: " + failures + ".");
                        clientManager.sendMessage(currentRequest.botRole, localizationLoader
                                .localize(Localizations.Service.POST_COMPLETED, currentRequest.botRole,
                                    new Localizations.Service.PostCompletedParams(successes, failures)));
                    } catch (ExecutionException e) {
                        LOGGER.error("An error has occured while waiting for a post request to complete.", e);
                        clientManager.sendMessage(currentRequest.botRole, localizationLoader
                                .localize(Localizations.Error.POST_REQUEST_FAILURE, currentRequest.botRole,
                                    new Localizations.Error.PostRequestFailureParams(successes, failures)));
                    }
                    currentRequest = null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void sendMessages(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        sendMessages0(botRole, parseRoleTypes(botRole, messages), messages.subList(0, messages.size() - 1));
    }

    public void sendMessages(BotRole botRole, RoleType roleType, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(roleType, "roleType cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        sendMessages0(botRole, Set.of(roleType), messages);
    }

    public void sendGeneralMessages(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        sendGeneralMessages0(botRole, parseRoleTypes(botRole, messages), messages.subList(0, messages.size() - 1));
    }

    public void sendGeneralMessages(BotRole botRole, RoleType roleType, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(roleType, "roleType cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        sendGeneralMessages0(botRole, Set.of(roleType), messages);
    }

    public void sendPrivateMessageToUser(BotRole botRole, Long targetId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        LOGGER.info("User " + botRole.getUser().getId() + " is sending a private message to user "
                + targetId + " in bot " + botRole.getBot().getId() + "...");
        final BotRole targetRole = entityUtil.getActiveBotRole(botRole, targetId);

        clientManager.sendMessage(targetRole, localizationLoader
                .localize(Localizations.Service.PRIVATE_MESSAGE_INFO, targetRole,
                    new Localizations.Service.PrivateMessageInfoParams(botRole.getUser().getFullName(),
                    entityUtil.getLocalizedTitle(targetRole, botRole))));
        LOGGER.debug("Info message sent.");
        contentService.sendContent(targetRole, contentService.parseAndPersistContent(targetRole, messages));
        LOGGER.debug("Content sent.");
    }

    @PreDestroy
    protected void stop() {
        postWorkerThreadExecutor.shutdownNow();
        LOGGER.info("Post executor has been ordered to shutdown.");
    }

    private synchronized void sendMessages0(BotRole botRole, Set<RoleType> roleTypes, List<Message> messages) {
        checkExecution(botRole);

        final LocalizedContent content = contentService.parseAndPersistContent(botRole, messages);

        LOGGER.info("Executing a post request from user " + botRole.getUser().getId() + " in bot "
                + botRole.getBot().getId() + "... Content id is " + content.getId() + ". Roles are " + roleTypes + ".");
        final long totalNumber = botRoleRepository.countByBotIdAndRoleTypeInAndIsDisabledFalse(botRole.getBot().getId(), roleTypes);
        final List<CompletableFuture<List<SendMessageResultDto>>> futures = new ArrayList<>();

        for (int i = 0; i < Math.ceil((double)totalNumber / QUERY_PAGE_SIZE); ++i) {
            final List<BotRole> botRoles = botRoleRepository.findByBotIdAndRoleTypeInAndIsDisabledFalse(botRole.getBot().getId(),
                    roleTypes, PageRequest.of(i, QUERY_PAGE_SIZE));

            futures.addAll(contentService.sendContentInBulkAsync(botRole, botRoles, content));
        }

        LOGGER.info("All of the requests have been initiated. Once they are all completed, a confirmation will be sent to "
                + botRole.getUser().getId() + ".");

        final CompletableFuture<Void> finalFuture = CompletableFuture.allOf(futures.stream()
                .map(f -> f.handle((r, t) -> null))
                .toArray(CompletableFuture[]::new));

        requestQueue.add(new Request(botRole, futures, finalFuture));

        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.POST_STARTED, botRole,
                new Localizations.Service.PostStartedParams(totalNumber)));
    }

    private synchronized void sendGeneralMessages0(BotRole botRole, Set<RoleType> roleTypes, List<Message> messages) {
        checkExecution(botRole);

        final LocalizedContent content = contentService.parseAndPersistContent(botRole, messages);

        LOGGER.info("Executing a general post request from director " + botRole.getUser().getId()
                + "... Content id is " + content.getId() + ". Roles are " + roleTypes + ".");
        final long totalNumber = botRoleRepository.countByRoleTypeInAndIsDisabledFalse(roleTypes);
        final List<CompletableFuture<List<SendMessageResultDto>>> futures = new ArrayList<>();

        for (int i = 0; i < Math.ceil((double)totalNumber / QUERY_PAGE_SIZE); ++i) {
            final List<BotRole> botRoles = botRoleRepository.findByRoleTypeInAndIsDisabledFalse(roleTypes,
                    PageRequest.of(i, QUERY_PAGE_SIZE));

            futures.addAll(contentService.sendContentInBulkAsync(botRole, botRoles, content));
        }

        LOGGER.info("All of the requests have been initiated. Once they are all completed, a confirmation will be sent to "
                + botRole.getUser().getId() + ".");

        final CompletableFuture<Void> finalFuture = CompletableFuture.allOf(futures.stream()
                .map(f -> f.handle((r, t) -> null))
                .toArray(CompletableFuture[]::new));

        requestQueue.add(new Request(botRole, futures, finalFuture));

        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.POST_STARTED, botRole,
                new Localizations.Service.PostStartedParams(totalNumber)));
    }

    private void checkExecution(BotRole botRole) {
        if (currentRequest != null && currentRequest.botRole.getId().equals(botRole.getId())
                || requestQueue.stream().anyMatch(r -> r.botRole.getId().equals(botRole.getId()))) {
            throw new ForbiddenOperationException("A request is already being executed. "
                    + "Only one is allowed per bot at a time.", localizationLoader.localize(
                    Error.TOO_MANY_POST_REQUESTS, botRole));
        }
    }

    private Set<RoleType> parseRoleTypes(BotRole botRole, List<Message> messages) {
        validatorUtil.checkAtLeastExpectedMessages(botRole, messages, 2);
        if (!messages.getLast().hasText()) {
            throw new InvalidDataSentException("The last message is supposed to be a list of roles.",
                    localizationLoader.localize(Localizations.Error.POST_NO_ROLES, botRole));
        }
        final String[] potentialRoleTypes = messages.getLast().getText().trim().split(" ");
        final Set<RoleType> roleTypes = new HashSet<>();

        for (final String roleStr : potentialRoleTypes) {
            try {
                roleTypes.add(RoleType.valueOf(roleStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new InvalidDataSentException("Unable to parse " + roleStr + " to a " + RoleType.class.getName() + " enum value", localizationLoader.localize(
                        Localizations.Error.PARSE_ROLE_TYPES_FAILURE, botRole, new Localizations.Error.ParseRoleTypesFailureParams(
                            Arrays.stream(RoleType.values()).map(t -> t.toString()).collect(Collectors.joining(" ")))), e);
            }
        }
        LOGGER.debug("Roles parsed.");

        return roleTypes;
    }

    private static class Request {
        BotRole botRole;

        List<CompletableFuture<List<SendMessageResultDto>>> futures;

        CompletableFuture<Void> finalFuture;

        Request(BotRole botRole, List<CompletableFuture<List<SendMessageResultDto>>> futures, CompletableFuture<Void> finalFuture) {
            this.botRole = botRole;
            this.futures = futures;  
            this.finalFuture = finalFuture;  
        }
    }
}

package com.unbidden.telegramcoursesbot.controller;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.config.properties.WebhookProperties;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.ExceptionHandlerManager;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.menu.MenuCallbackRequestProcessor;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.orchestration.PaymentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.SessionDistributor;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook")
public class WebhookController {
    private static final Logger LOGGER = LogManager.getLogger(WebhookController.class);

    private static final String SECRET_KEY_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final CommandHandlerManager commandHandlerManager;

    private final ExceptionHandlerManager exceptionHandlerManager;

    private final PaymentOrchestrationService paymentService;

    private final MenuCallbackRequestProcessor callbackRequestProcessor;

    private final MenuOrchestrationService menuService;

    private final SessionDistributor sessionDistributor;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    private final WebhookProperties webhookProperties;

    @PostMapping("/callback/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long id, @RequestBody Update update,
            HttpServletRequest request) {
        if (!doesSecretMatch(request)) {
            LOGGER.warn("A request with an incorrect secret key was sent. It will be ignored.");
            return;
        }
        BotRole botRole = null;

        try {
            if (update.hasMessage() && update.getMessage().isCommand()) {
                final String[] commandParts = update.getMessage().getText().split(" ");

                botRole = userService.initializeUserForBot(update.getMessage().getFrom(), id);
                if (botRole.getUser().isBanned()) {
                    LOGGER.trace("User " + botRole.getUser().getId() + " is banned. The request will be ignored.");
                    return;
                }
                checkMaintenance(botRole);

                sessionDistributor.removeSessionsForUser(botRole);

                LOGGER.debug("Update with command " + update.getMessage().getText()
                        + " triggered by user " + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ".");
                commandHandlerManager.getHandler(commandParts[0]).handle(botRole, update.getMessage(), commandParts);
            } else if (update.hasPreCheckoutQuery()) {
                botRole = userService.initializeUserForBot(update.getPreCheckoutQuery().getFrom(), id);
                if (botRole.getUser().isBanned()) {
                    LOGGER.trace("User " + botRole.getUser().getId() + " is banned. The request will be ignored.");
                    return;
                }
                checkMaintenance(botRole);

                sessionDistributor.removeSessionsForUser(botRole);

                LOGGER.debug("Update with a precheckout query triggered by user "
                        + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ".");
                paymentService.resolvePreCheckout(botRole, update.getPreCheckoutQuery());
            } else if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
                botRole = userService.initializeUserForBot(update.getMessage().getFrom(), id);
                if (botRole.getUser().isBanned()) {
                    return;
                }

                sessionDistributor.removeSessionsForUser(botRole);

                LOGGER.debug("Update with a successful payment triggered by user "
                        + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ".");
                paymentService.resolveSuccessfulPayment(botRole, update.getMessage().getSuccessfulPayment());
            } else if (update.hasCallbackQuery()) {
                botRole = userService.initializeUserForBot(update.getCallbackQuery().getFrom(), id);
                if (botRole.getUser().isBanned()) {
                    return;
                }
                checkMaintenance(botRole, update.getCallbackQuery());

                LOGGER.debug("Update with a callback query triggered by user "
                        + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ". Button "
                        + update.getCallbackQuery().getData() + ".");
                callbackRequestProcessor.processCallbackQuery(botRole, update.getCallbackQuery());
            } else if (update.hasMessage()) {
                botRole = userService.initializeUserForBot(update.getMessage().getFrom(), id);
                if (botRole.getUser().isBanned()) {
                    return;
                }
                checkMaintenance(botRole);
                
                LOGGER.debug("Update with a general message was sent by user "
                        + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ".");
                sessionDistributor.callService(botRole, update.getMessage());
            } else if (update.hasMyChatMember()) {
                botRole = userService.initializeUserForBot(update.getMyChatMember().getFrom(), id);

                if (update.getMyChatMember().getNewChatMember().getStatus().equals("kicked")) {
                    LOGGER.info("User " + botRole.getUser().getId() + " has blocked bot " + botRole.getBot().getId() + ".");
                    userService.disableUser(botRole);
                } else {
                    LOGGER.info("User " + botRole.getUser().getId() + " has activated bot " + botRole.getBot().getId() + ".");
                }
            }
        } catch (Exception e) { 
            if (botRole != null) {
                clientManager.sendMessage(botRole, exceptionHandlerManager.handleException(botRole, e));
                sessionDistributor.removeSessionsWithoutConfirmationForUser(botRole);
            } else {
                LOGGER.error("An exception occured before the user could be loaded. "
                        + "This likely indicates a bug.", e);
                
                clientManager.sendMessage(botRole, exceptionHandlerManager
                        .handleException(entityUtil.getDirectorBotRole(id), e));
            }
        }
        if (botRole != null) {
            try {
                menuService.answerPotentialCallbackQuery(botRole);
            } catch (CallbackQueryAnswerException e) {
                LOGGER.error("Callback query exception occured in bot " + botRole.getBot().getId()
                        + ". Some investigation might be required", e);
                clientManager.sendMessage(botRole, exceptionHandlerManager.handleException(
                        entityUtil.getDirectorBotRole(botRole.getBot().getId()), e));
            }
        }
    }

    @PostMapping("/botlord")
    public void botLordUpdate(@RequestBody Update update, HttpServletRequest request) {
        if (!doesSecretMatch(request)) {
            LOGGER.warn("A request with an incorrect secret key was sent. It will be ignored.");
            return;
        }
        BotRole botRole = null;

        try {
            if (update.hasMessage() && update.getMessage().isCommand()) {
                final String[] commandParts = update.getMessage().getText().split(" ");

                botRole = userService.initializeUserForBot(update.getMessage().getFrom(), EntityUtil.BOT_LORD_ID);
                if (!isDirector(botRole)) {
                    return;
                }

                sessionDistributor.removeSessionsForUser(botRole);

                LOGGER.debug("Update with command " + update.getMessage().getText()
                        + " was sent in bot lord.");
                commandHandlerManager.getHandler(commandParts[0]).handle(botRole, update.getMessage(), commandParts);
            } else if (update.hasCallbackQuery()) {
                botRole = userService.initializeUserForBot(update.getCallbackQuery().getFrom(), EntityUtil.BOT_LORD_ID);
                if (!isDirector(botRole)) {
                    return;
                }

                LOGGER.debug("Update with callback query was sent in bot lord. Button "
                        + update.getCallbackQuery().getData() + ".");
                callbackRequestProcessor.processCallbackQuery(botRole, update.getCallbackQuery());
            } else if (update.hasMessage()) {
                botRole = userService.initializeUserForBot(update.getMessage().getFrom(), EntityUtil.BOT_LORD_ID);
                if (!isDirector(botRole)) {
                    return;
                }

                LOGGER.debug("Update with a general message was sent by user "
                        + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ".");
                sessionDistributor.callService(botRole, update.getMessage());
            }
        } catch (Exception e) { 
            if (botRole != null) {
                clientManager.sendMessage(botRole, exceptionHandlerManager.handleException(botRole, e));
                sessionDistributor.removeSessionsWithoutConfirmationForUser(botRole);
            }
        }
        if (botRole != null) {
            try {
                menuService.answerPotentialCallbackQuery(botRole);
            } catch (CallbackQueryAnswerException e) {
                LOGGER.error("Callback query exception occured in bot lord. Some investigation "
                        + "might be required", e);
                clientManager.sendMessage(botRole, localizationLoader
                        .localize(Error.BOTLORD_CALLBACK_EXCEPTION, botRole));
            }
        }
    }

    @GetMapping("/info")
    public String getWebhookInfo(@RequestParam Long id) {
        final Bot bot = entityUtil.getBot(id);

        return clientManager.getClient(bot).getInfo().toString();
    }

    private void checkMaintenance(BotRole botRole) {
        checkMaintenance(botRole, null);
    }

    private void checkMaintenance(BotRole botRole, CallbackQuery query) {
        if (clientManager.isOnMaintenance()) {
            if (query != null) {
                try {
                    clientManager.getClient(botRole.getBot()).execute(AnswerCallbackQuery.builder().callbackQueryId(query.getId()).build());
                } catch (TelegramApiException e) {
                    LOGGER.error("Failed to answer a callback query after denying access due to maintenance.", e);
                }
            }
            throw new OnMaintenanceException("Server is on maintenance", localizationLoader
                    .localize(Error.SERVER_ON_MAINTENANCE, botRole));
        }
    }
    
    private boolean isDirector(BotRole botRole) {
        return botRole.getRole().getType() == RoleType.DIRECTOR;
    }

    private boolean doesSecretMatch(HttpServletRequest request) {
        return webhookProperties.secret().equals(request.getHeader(SECRET_KEY_HEADER));
    }
}

package com.unbidden.telegramcoursesbot.controller;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.ExceptionHandlerManager;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.orchestration.PaymentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.SessionDistributor;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook")
public class WebhookController {
    private static final Logger LOGGER = LogManager.getLogger(WebhookController.class);

    private static final String SECRET_KEY_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final CommandHandlerManager commandHandlerManager;

    private final ExceptionHandlerManager exceptionHandlerManager;

    private final PaymentOrchestrationService paymentService;

    private final MenuService menuService;

    private final SessionDistributor sessionDistributor;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Value("${telegram.bot.webhook.secret}")
    private String secretKey;

    @PostMapping("/callback/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long id, @RequestBody Update update,
            HttpServletRequest request) {
        if (!doesSecretMatch(request)) {
            LOGGER.warn("A request with an incorrect secret key was sent. It will be ignored.");
            return;
        }
        final Bot bot = entityUtil.getBot(id);
        UserEntity user = null;

        try {
            if (update.hasMessage() && update.getMessage().isCommand()) {
                final String[] commandParts = update.getMessage().getText().split(" ");

                user = userService.initializeUserForBot(update.getMessage().getFrom(), bot);
                if (user.isBanned()) {
                    return;
                }
                checkMaintenance(user);

                sessionDistributor.removeSessionsForUser(user, bot);

                LOGGER.debug("Update with command " + update.getMessage().getText()
                        + " triggered by user " + user.getId() + " in bot " + bot.getId() + ".");
                commandHandlerManager.getHandler(commandParts[0]).handle(user, bot, update.getMessage(), commandParts);
            } else if (update.hasPreCheckoutQuery()) {
                user = userService.initializeUserForBot(update.getPreCheckoutQuery().getFrom(),
                        bot);
                if (user.isBanned()) {
                    return;
                }
                checkMaintenance(user);

                sessionDistributor.removeSessionsForUser(user, bot);

                LOGGER.debug("Update with a precheckout query triggered by user "
                        + user.getId() + " in bot " + bot.getId() + ".");
                paymentService.resolvePreCheckout(user, bot, update.getPreCheckoutQuery());
            } else if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
                user = userService.initializeUserForBot(update.getMessage().getFrom(), bot);
                if (user.isBanned()) {
                    return;
                }

                sessionDistributor.removeSessionsForUser(user, bot);

                LOGGER.debug("Update with a successful payment triggered by user "
                        + user.getId() + " in bot " + bot.getId() + ".");
                paymentService.resolveSuccessfulPayment(user, bot, update.getMessage().getSuccessfulPayment());
            } else if (update.hasCallbackQuery()) {
                user = userService.initializeUserForBot(update.getCallbackQuery().getFrom(), bot);
                if (user.isBanned()) {
                    return;
                }
                checkMaintenance(user);

                LOGGER.debug("Update with a callback query triggered by user "
                        + user.getId() + " in bot " + bot.getId() + ". Button "
                        + update.getCallbackQuery().getData() + ".");
                menuService.processCallbackQuery(update.getCallbackQuery(), bot);
            } else if (update.hasMessage()) {
                user = userService.initializeUserForBot(update.getMessage().getFrom(), bot);
                if (user.isBanned()) {
                    return;
                }
                checkMaintenance(user);
                
                LOGGER.debug("Update with a general message was sent by user "
                        + user.getId() + " in bot " + bot.getId() + ".");
                sessionDistributor.callService(user, bot, update.getMessage());
            }
        } catch (Exception e) { 
            if (user != null) {
                clientManager.getClient(bot).sendMessage(exceptionHandlerManager
                        .handleException(user, bot, e));
                sessionDistributor.removeSessionsWithoutConfirmationForUser(user, bot);
            } else {
                LOGGER.error("An exception occured before the user could be loaded. "
                        + "This likely indicates a bug.", e);
                
                clientManager.getBotLordClient().sendMessage(exceptionHandlerManager
                        .handleException(entityUtil.getDiretor(), bot, e));
            }
        }
        if (user != null) {
            try {
                menuService.answerPotentialCallbackQuery(user, bot);
            } catch (CallbackQueryAnswerException e) {
                LOGGER.error("Callback query exception occured in bot " + bot.getId()
                        + ". Some investigation might be required", e);
                clientManager.getBotLordClient().sendMessage(exceptionHandlerManager
                        .handleException(entityUtil.getDiretor(), bot, e));
            }
        } else {
            LOGGER.error("Unable to answer a callback query because the user is unknown.");
            clientManager.getBotLordClient().sendMessage(exceptionHandlerManager
                    .handleException(entityUtil.getDiretor(), bot, new RuntimeException(
                    "Unable to answer a callback query because the user is unknown.")));
        }
    }

    @PostMapping("/botlord")
    public void botLordUpdate(@RequestBody Update update, HttpServletRequest request) {
        if (!doesSecretMatch(request)) {
            LOGGER.warn("A request with an incorrect secret key was sent. It will be ignored.");
            return;
        }
        final Bot bot = entityUtil.getBotLord();
        UserEntity user = null;

        try {
            if (update.hasMessage() && update.getMessage().isCommand()) {
                final String[] commandParts = update.getMessage().getText().split(" ");

                user = userService.initializeUserForBot(update.getMessage().getFrom(), bot);
                if (!isDirector(user)) {
                    return;
                }

                sessionDistributor.removeSessionsForUser(user, bot);

                LOGGER.debug("Update with command " + update.getMessage().getText()
                        + " was sent in bot lord.");
                commandHandlerManager.getHandler(commandParts[0]).handle(user, bot,
                        update.getMessage(), commandParts);
            } else if (update.hasCallbackQuery()) {
                user = userService.initializeUserForBot(update.getCallbackQuery().getFrom(), bot);
                if (!isDirector(user)) {
                    return;
                }

                LOGGER.debug("Update with callback query was sent in bot lord. Button "
                        + update.getCallbackQuery().getData() + ".");
                menuService.processCallbackQuery(update.getCallbackQuery(), bot);
            } else if (update.hasMessage()) {
                user = userService.initializeUserForBot(update.getMessage().getFrom(), bot);
                if (!isDirector(user)) {
                    return;
                }

                LOGGER.debug("Update with a general message was sent by user "
                        + user.getId() + " in bot " + bot.getId() + ".");
                sessionDistributor.callService(user, bot, update.getMessage());
            }
        } catch (Exception e) { 
            if (user != null) {
                clientManager.getBotLordClient().sendMessage(exceptionHandlerManager
                        .handleException(user, bot, e));
                sessionDistributor.removeSessionsWithoutConfirmationForUser(user, bot);
            }
        }
        if (user != null) {
            try {
                menuService.answerPotentialCallbackQuery(user, bot);
            } catch (CallbackQueryAnswerException e) {
                LOGGER.error("Callback query exception occured in bot lord. Some investigation "
                        + "might be required", e);
                clientManager.getBotLordClient().sendMessage(user, localizationLoader
                        .localize(Error.BOTLORD_CALLBACK_EXCEPTION, user));
            }
        }
    }

    @GetMapping("/info")
    public String getWebhookInfo(@RequestParam Long id) {
        final Bot bot = entityUtil.getBot(id);

        return clientManager.getClient(bot).getInfo().toString();
    }

    private void checkMaintenance(UserEntity user) {
        if (clientManager.isOnMaintenance()) {
            throw new OnMaintenanceException("Server is on maintenance", localizationLoader
                    .localize(Error.SERVER_ON_MAINTENANCE, user));
        }
    }
    
    private boolean isDirector(@NonNull UserEntity user) {
        return entityUtil.getDiretor().getId().equals(user.getId());
    }

    private boolean doesSecretMatch(HttpServletRequest request) {
        return secretKey.equals(request.getHeader(SECRET_KEY_HEADER));
    }
}

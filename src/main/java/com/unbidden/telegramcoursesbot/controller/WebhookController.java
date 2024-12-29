package com.unbidden.telegramcoursesbot.controller;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.ExceptionHandlerManager;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.session.SessionDistributor;
import com.unbidden.telegramcoursesbot.service.user.UserService;
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

    private static final String ERROR_SERVER_ON_MAINTENANCE = "error_server_on_maintenance";
    private static final String ERROR_BOTFATHER_CALLBACK_EXCEPTION =
            "error_botfather_callback_exception";

    private static final String SECRET_KEY_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final CommandHandlerManager commandHandlerManager;

    private final ExceptionHandlerManager exceptionHandlerManager;

    private final PaymentService paymentService;

    private final MenuService menuService;

    private final SessionDistributor sessionDistributor;

    private final UserService userService;

    private final BotService botService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Value("${telegram.bot.webhook.secret}")
    private String secretKey;

    @PostMapping("/callback/{botName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String botName, @RequestBody Update update,
            HttpServletRequest request) {
        if (!doesSecretMatch(request)) {
            LOGGER.warn("A request with incorrect secret key was sent. It will be ignored.");
            return;
        }
        final Bot bot = botService.getBot(botName);
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
                        + " triggered by user " + user.getId() + " in bot "
                        + bot.getName() + ".");
                commandHandlerManager.getHandler(commandParts[0]).handle(bot, user,
                        update.getMessage(), commandParts);
            } else if (update.hasPreCheckoutQuery()) {
                user = userService.initializeUserForBot(update.getPreCheckoutQuery().getFrom(),
                        bot);
                if (user.isBanned()) {
                    return;
                }
                checkMaintenance(user);

                sessionDistributor.removeSessionsForUser(user, bot);

                LOGGER.debug("Update with precheckout query triggered by user "
                        + user.getId() + " in bot " + bot.getName() + ".");
                paymentService.resolvePreCheckout(update.getPreCheckoutQuery(), bot);
            } else if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
                user = userService.initializeUserForBot(update.getMessage().getFrom(), bot);
                if (user.isBanned()) {
                    return;
                }

                sessionDistributor.removeSessionsForUser(user, bot);

                LOGGER.debug("Update with successful payment triggered by user "
                        + user.getId() + " in bot " + bot.getName() + ".");
                paymentService.resolveSuccessfulPayment(update.getMessage(), bot);
            } else if (update.hasCallbackQuery()) {
                user = userService.initializeUserForBot(update.getCallbackQuery().getFrom(), bot);
                if (user.isBanned()) {
                    return;
                }
                checkMaintenance(user);

                LOGGER.debug("Update with callback query triggered by user "
                        + user.getId() + " in bot " + bot.getName() + ". Button "
                        + update.getCallbackQuery().getData() + ".");
                menuService.processCallbackQuery(update.getCallbackQuery(), bot);
            } else if (update.hasMessage()) {
                user = userService.initializeUserForBot(update.getMessage().getFrom(), bot);
                if (user.isBanned()) {
                    return;
                }
                checkMaintenance(user);
                
                LOGGER.debug("Update with a general message was sent by user "
                        + user.getId() + " in bot " + bot.getName() + ".");
                sessionDistributor.callService(update.getMessage(), user, bot);
            }
        } catch (Exception e) { 
            if (user != null) {
                clientManager.getClient(bot).sendMessage(exceptionHandlerManager
                        .handleException(user, bot, e));
                sessionDistributor.removeSessionsWithoutConfirmationForUser(user, bot);
            } else {
                LOGGER.error("Strange situation occured - unable to handle "
                        + "exception due to the user being unknown. Theoretically, "
                        + "this should not be possible. Investigate immediately.", e);
                
                clientManager.getBotFatherClient().sendMessage(exceptionHandlerManager
                        .handleException(userService.getDiretor(), bot, e));
            }
        }
        if (user != null) {
            try {
                menuService.answerPotentialCallbackQuery(user, bot);
            } catch (CallbackQueryAnswerException e) {
                LOGGER.error("Callback query exception occured in bot " + bot.getName()
                        + ". Some investigation might be required", e);
                clientManager.getBotFatherClient().sendMessage(exceptionHandlerManager
                        .handleException(userService.getDiretor(), bot, e));
            }
        } else {
            LOGGER.error("Unable to answer callback query because user is unknown");
            clientManager.getBotFatherClient().sendMessage(exceptionHandlerManager
                    .handleException(userService.getDiretor(), bot, new RuntimeException(
                    "Weird shit is happening dude!")));
        }
    }

    @PostMapping("/botfather")
    public void botFatherUpdate(@RequestBody Update update, HttpServletRequest request) {
        if (!doesSecretMatch(request)) {
            LOGGER.warn("A request with incorrect secret key was sent. It will be ignored.");
            return;
        }
        final Bot bot = botService.getBotFather();
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
                        + " was sent in botfather.");
                commandHandlerManager.getHandler(commandParts[0]).handle(bot, user,
                        update.getMessage(), commandParts);
            } else if (update.hasCallbackQuery()) {
                user = userService.initializeUserForBot(update.getCallbackQuery().getFrom(), bot);
                if (!isDirector(user)) {
                    return;
                }

                LOGGER.debug("Update with callback query was sent in botfather. Button "
                        + update.getCallbackQuery().getData() + ".");
                menuService.processCallbackQuery(update.getCallbackQuery(), bot);
            } else if (update.hasMessage()) {
                user = userService.initializeUserForBot(update.getMessage().getFrom(), bot);
                if (!isDirector(user)) {
                    return;
                }

                LOGGER.debug("Update with a general message was sent by user "
                        + user.getId() + " in bot " + bot.getName() + ".");
                sessionDistributor.callService(update.getMessage(), user, bot);
            }
        } catch (Exception e) { 
            if (user != null) {
                clientManager.getBotFatherClient().sendMessage(exceptionHandlerManager
                        .handleException(user, bot, e));
                sessionDistributor.removeSessionsWithoutConfirmationForUser(user, bot);
            }
        }
        if (user != null) {
            try {
                menuService.answerPotentialCallbackQuery(user, bot);
            } catch (CallbackQueryAnswerException e) {
                LOGGER.error("Callback query exception occured in botfather. Some investigation "
                        + "might be required", e);
                clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                        .getLocalizationForUser(ERROR_BOTFATHER_CALLBACK_EXCEPTION, user));
            }
        }
    }

    @GetMapping("/info")
    public String getWebhookInfo(@RequestParam String botName) {
        final Bot bot = botService.getBot(botName);

        return clientManager.getClient(bot).getInfo().toString();
    }

    private void checkMaintenance(UserEntity user) {
        if (clientManager.isOnMaintenance()) {
            throw new OnMaintenanceException("Server is on maintenance", localizationLoader
                    .getLocalizationForUser(ERROR_SERVER_ON_MAINTENANCE, user));
        }
    }
    
    private boolean isDirector(@NonNull UserEntity user) {
        return userService.getDiretor().getId().equals(user.getId());
    }

    private boolean doesSecretMatch(HttpServletRequest request) {
        return request.getHeader(SECRET_KEY_HEADER).equals(secretKey);
    }
}

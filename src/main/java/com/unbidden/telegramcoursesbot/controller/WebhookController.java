package com.unbidden.telegramcoursesbot.controller;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.ExceptionHandlerManager;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.CallbackQueryRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.session.SessionDistributor;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    private final CallbackQueryRepository callbackQueryRepository;

    private final CommandHandlerManager commandHandlerManager;

    private final ExceptionHandlerManager exceptionHandlerManager;

    private final PaymentService paymentService;

    private final MenuService menuService;

    private final SessionDistributor sessionDistributor;

    private final UserService userService;

    private final CustomTelegramClient client;

    @Value("${telegram.bot.webhook.secret}")
    private String secretKey;

    @PostMapping("/callback/bot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@RequestBody Update update, HttpServletRequest request) {
        if (!doesSecretMatch(request)) {
            LOGGER.warn("A request with incorrect secret key was sent. It will be ignored.");
            return;
        }
        UserEntity user = null;
        
        try {
            if (update.hasMessage() && update.getMessage().isCommand()) {
                final String[] commandParts = update.getMessage().getText().split(" ");
                user = userService.updateUser(update.getMessage().getFrom());
                sessionDistributor.removeSessionsForUser(user);

                LOGGER.info("Update with command " + update.getMessage().getText()
                        + " triggered by user " + user.getId() + ".");
                commandHandlerManager.getHandler(commandParts[0]).handle(update.getMessage(),
                        commandParts);
            } else if (update.hasPreCheckoutQuery()) {
                user = userService.updateUser(update.getPreCheckoutQuery().getFrom());
                sessionDistributor.removeSessionsForUser(user);

                LOGGER.info("Update with precheckout query triggered by user "
                        + user.getId() + ".");
                paymentService.resolvePreCheckout(update.getPreCheckoutQuery());
            } else if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
                user = userService.updateUser(update.getMessage().getFrom());
                sessionDistributor.removeSessionsForUser(user);

                LOGGER.info("Update with successful payment triggered by user "
                        + user.getId() + ".");
                paymentService.resolveSuccessfulPayment(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                user = userService.updateUser(update.getCallbackQuery().getFrom());

                LOGGER.info("Update with callback query triggered by user "
                        + user.getId() + ". Button " + update.getCallbackQuery().getData() + ".");
                menuService.processCallbackQuery(update.getCallbackQuery());
            } else if (update.hasMessage()) {
                user = userService.updateUser(update.getMessage().getFrom());

                sessionDistributor.callService(update.getMessage());
            }
        } catch (Exception e) { 
            if (user != null) {
                client.sendMessage(exceptionHandlerManager.handleException(user, e));
                sessionDistributor.removeSessionsWithoutConfirmationForUser(user);
            } else {
                LOGGER.error("Strange situation occured - unable to handle "
                        + "exception due to the user being unknown. Theoretically, "
                        + "this should not be possible. Investigate immediately.", e);
                
                client.sendMessage(exceptionHandlerManager.handleException(userService.getDiretor(), e));
            }
        }
        answerPotentialCallbackQuery(user);
    }

    @GetMapping("/test")
    public String test() {
        return client.getInfo().toString();
    }

    private void answerPotentialCallbackQuery(UserEntity user) {
        if (user != null) {
            final Optional<CallbackQuery> query = callbackQueryRepository
                    .findAndRemove(user.getId());
            if (query.isPresent()) {
                LOGGER.debug("User " + user.getId() + " has an unanswered callback query.");
                try {
                    client.execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(query.get().getId())
                            .build());
                    LOGGER.debug("Callback query resolved.");
                } catch (TelegramApiException e) {
                    LOGGER.error("Unable to answer callback query. This should not break "
                            + "anything but should be invesigated.", e);
                    
                    client.sendMessage(exceptionHandlerManager.handleException(
                            userService.getDiretor(), e));
                }
            }
        }
    }

    private boolean doesSecretMatch(HttpServletRequest request) {
        return request.getHeader(SECRET_KEY_HEADER).equals(secretKey);
    }
}

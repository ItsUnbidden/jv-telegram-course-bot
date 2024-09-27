package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger LOGGER = LogManager.getLogger(TelegramBot.class);

    @Value("${telegram.bot.authorization.username}")
    private String username;

    private volatile boolean isOnMaintenance;

    @Autowired
    @Lazy
    private CommandHandlerManager commandHandlerManager;

    @Autowired
    @Lazy
    private PaymentService paymentService;

    @Autowired
    @Lazy
    private MenuService menuService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private LocalizationLoader localizationLoader;

    public TelegramBot(@Autowired DefaultBotOptions botOptions,
            @Value("${telegram.bot.authorization.token}") String token) {
        super(botOptions, token);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().isCommand()) {
            final String[] commandParts = update.getMessage().getText().split(" ");

            LOGGER.info("Update with command " + update.getMessage().getText()
                    + " triggered by user " + update.getMessage().getFrom().getId() + ".");
            sessionService.removeSessionsForUser(update.getMessage().getFrom());
            commandHandlerManager.getHandler(commandParts[0]).handle(update.getMessage(),
                    commandParts);
            return;
        }
        if (update.hasPreCheckoutQuery()) {
            sessionService.removeSessionsForUser(update.getPreCheckoutQuery().getFrom());
            paymentService.resolvePreCheckout(update.getPreCheckoutQuery());
            return;
        }
        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            sessionService.removeSessionsForUser(update.getMessage().getFrom());
            paymentService.resolveSuccessfulPayment(update.getMessage());
            return;
        }
        if (update.hasCallbackQuery()) {
            sessionService.removeSessionsForUser(update.getCallbackQuery().getFrom());
            menuService.processCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage()) {
            sessionService.processResponse(update.getMessage());
            return;
        }
    }

    public void setUpMenuButton() {
        SetChatMenuButton setChatMenuButton = SetChatMenuButton.builder()
                .menuButton(MenuButtonCommands.builder().build())
                .build();
        try {
            execute(setChatMenuButton);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set bot's menu button.", e);
        }
    }

    public void setUpMenus() {
        localizationLoader.getAvailableLanguageCodes().forEach(c -> setUpMenu(c));
    }

    public void setUpMenusForAdmin(@NonNull Long userId) {
        localizationLoader.getAvailableLanguageCodes().forEach(c ->
                setUpMenuForAdmin(userId, c));
    }

    public void removeMenusForUser(@NonNull Long userId) {
        localizationLoader.getAvailableLanguageCodes().forEach(c ->
                deleteAdminMenuForUser(userId, c));
    }

    private void setUpMenu(String languageCode) {
        final List<BotCommand> userCommands = parseToBotCommands(commandHandlerManager
                .getUserCommands(), languageCode);

        SetMyCommands setMyUserCommands = SetMyCommands.builder()
                .commands(userCommands)
                .scope(BotCommandScopeDefault.builder().build())
                .languageCode(languageCode)
                .build();
        try {
            execute(setMyUserCommands);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up a menu.", e);
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    public Message sendMessage(SendMessage sendMessage) {
        try {
            return execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send message.", e);
        }
    }

    public boolean isOnMaintenance() {
        return isOnMaintenance;
    }

    public void setOnMaintenance(boolean isOnMaintenance) {
        this.isOnMaintenance = isOnMaintenance;
    }

    private void deleteAdminMenuForUser(@NonNull Long userId, @NonNull String languageCode) {
        try {
            execute(DeleteMyCommands.builder()
                    .languageCode(languageCode)
                    .scope(BotCommandScopeChat.builder()
                        .chatId(userId).build())
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to clear commands for user " + userId
                    + " and language code " + languageCode, e);
        }
    }

    private void setUpMenuForAdmin(@NonNull Long userId, @NonNull String languageCode) {
        final List<BotCommand> userCommands = parseToBotCommands(commandHandlerManager
                .getUserCommands(), languageCode);
        final List<BotCommand> adminCommands = new ArrayList<>(parseToBotCommands(
                commandHandlerManager.getAdminCommands(), languageCode));
        adminCommands.addAll(userCommands);

        try {
            execute(SetMyCommands.builder().commands(adminCommands)
                    .scope(BotCommandScopeChat.builder().chatId(userId).build())
                    .languageCode(languageCode).build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set admin commands for user " + userId
                    + " and language code " + languageCode, e);
        }
    }

    private List<BotCommand> parseToBotCommands(List<String> commands, String languageCode) {
        return commands.stream()
                .map(c -> BotCommand.builder()
                    .command(c)
                    .description(localizationLoader.loadLocalization(
                        "menu_command_" + c.replace("/", "") + "_description",
                        languageCode).getData())
                    .build())
                .toList();
    }
}

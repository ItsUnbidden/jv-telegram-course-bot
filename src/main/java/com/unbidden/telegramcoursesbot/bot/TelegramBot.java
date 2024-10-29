package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.GetWebhookInfo;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.WebhookInfo;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramWebhookBot {
    private static final String UPDATE_ENDPOINT_PATH = "bot";

    private static final String MENU_COMMAND_DESCRIPTION = "menu_command_%s_description";
    
    private static final Logger LOGGER = LogManager.getLogger(TelegramBot.class);

    private static final List<String> COMMAND_MENU_EXCEPTIONS = new ArrayList<>();

    @Value("${telegram.bot.authorization.username}")
    private String username;

    private volatile boolean isOnMaintenance;

    @Autowired
    private LocalizationLoader localizationLoader;

    public TelegramBot(@Autowired DefaultBotOptions botOptions,
            @Value("${telegram.bot.authorization.token}") String token) {
        super(botOptions, token);
        COMMAND_MENU_EXCEPTIONS.add("/testcourse");
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        LOGGER.info("On webhook update received method is not supported.");
        return null;
    }

    @Override
    public String getBotPath() {
        return UPDATE_ENDPOINT_PATH;
    }

    public WebhookInfo getInfo() {
        try {
            return execute(GetWebhookInfo.builder().build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to get webhook info.", e);
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

    public void setUpUserMenu(@NonNull String languageCode,
            @NonNull List<String> userCommandNames) {
        final List<BotCommand> userCommands = parseToBotCommands(userCommandNames, languageCode);

        final SetMyCommands setMyUserCommands = SetMyCommands.builder()
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

    public void deleteAdminMenuForUser(@NonNull UserEntity user) {
        try {
            execute(DeleteMyCommands.builder()
                    .languageCode(user.getLanguageCode())
                    .scope(BotCommandScopeChat.builder()
                        .chatId(user.getId()).build())
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to clear commands for user " + user.getId()
                    + " and language code " + user.getLanguageCode(), e);
        }
        LOGGER.info("Admin menu for user " + user.getId() + " has been removed.");
    }

    public void setUpMenuForAdmin(@NonNull UserEntity user, @NonNull List<String> allCommands) {
        final List<BotCommand> adminCommands = parseToBotCommands(allCommands,
                user.getLanguageCode());

        try {
            execute(SetMyCommands.builder().commands(adminCommands)
                    .scope(BotCommandScopeChat.builder().chatId(user.getId()).build())
                    .languageCode(user.getLanguageCode()).build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set admin commands for user " + user.getId()
                    + " and language code " + user.getLanguageCode(), e);
        }
        LOGGER.info("Admin menu for user " + user.getId() + " has been added.");
    }

    private List<BotCommand> parseToBotCommands(List<String> commands, String languageCode) {
        return commands.stream()
                .filter(c -> !COMMAND_MENU_EXCEPTIONS.contains(c))
                .map(c -> BotCommand.builder()
                    .command(c)
                    .description(localizationLoader.loadLocalization(
                        MENU_COMMAND_DESCRIPTION.formatted(c.replace("/", "")),
                        languageCode).getData())
                    .build())
                .toList();
    }
}

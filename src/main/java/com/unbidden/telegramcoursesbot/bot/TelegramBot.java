package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.User;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger LOGGER = LogManager.getLogger(TelegramBot.class);

    private static final List<BotCommand> BOT_COMMANDS = new ArrayList<>();

    private static final List<User> ADMIN_LIST = new ArrayList<>(); 

    @Value("${telegram.bot.authorization.username}")
    private String username;

    @Value("${telegram.bot.authorization.default.admin.id}")
    private String defaultAdminId;

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
    private UserRepository userRepository;

    public TelegramBot(@Autowired DefaultBotOptions botOptions,
            @Value("${telegram.bot.authorization.token}") String token) {
        super(botOptions, token);
    }

    @PostConstruct
    private void init() {
        ADMIN_LIST.add(userRepository.findById(Long.parseLong(defaultAdminId)).get());

        BOT_COMMANDS.add(BotCommand.builder()
                .command("/start")
                .description("Initiates the bot")
                .build());
        BOT_COMMANDS.add(BotCommand.builder()
                .command("/terms")
                .description("Shows terms of service")
                .build());
        BOT_COMMANDS.add(BotCommand.builder()
                .command("/inlinebuttons")
                .description("Shows message with buttons.")
                .build());
        BOT_COMMANDS.add(BotCommand.builder()
                .command("/keyboard")
                .description("Shows custom keyboard.")
                .build());

        setUpCommands();
        setUpMenu();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().isCommand()) {
            final String[] commandParts = update.getMessage().getText().split(" ");

            LOGGER.info("Update with command " + update.getMessage().getText()
                    + " triggered by user " + update.getMessage().getFrom().getId() + ".");
            commandHandlerManager.getHandler(commandParts[0]).handle(update.getMessage(),
                    commandParts);
            return;
        }
        if (update.hasPreCheckoutQuery()) {
            paymentService.resolvePreCheckout(update.getPreCheckoutQuery());
        }
        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            paymentService.resolveSuccessfulPayment(update.getMessage());
        }
        if (update.hasCallbackQuery()) {
            menuService.processCallbackQuery(update.getCallbackQuery());
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

    public boolean isAdmin(User user) {
        final boolean isAdmin = !ADMIN_LIST.stream()
                .filter(u -> u.getId().longValue() == user.getId().longValue())
                .toList()
                .isEmpty();
        if (!isAdmin) {
            LOGGER.warn("User " + user.getId()
                    + " tried to access admin command without admin access.");
        }
        return isAdmin;
    }

    private void setUpMenu() {
        SetChatMenuButton setChatMenuButton = SetChatMenuButton.builder()
                .menuButton(MenuButtonCommands.builder().build())
                .build();

        try {
            execute(setChatMenuButton);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Unable to set up the menu.", e);
        }
    }

    private void setUpCommands() {
        SetMyCommands setMyCommands = SetMyCommands.builder()
                .scope(BotCommandScopeDefault.builder().build())
                .commands(BOT_COMMANDS)
                .build();
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Unable to set up the commands.", e);
        } 
    }
}

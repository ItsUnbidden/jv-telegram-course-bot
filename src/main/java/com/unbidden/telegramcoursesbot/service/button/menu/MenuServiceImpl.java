package com.unbidden.telegramcoursesbot.service.button.menu;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.KeyboardUtil;
import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup.EditMessageReplyMarkupBuilder;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText.EditMessageTextBuilder;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {
    private static final Logger LOGGER = LogManager.getLogger(MenuServiceImpl.class);

    private static final String DIVIDER = ":";

    private static final int MENU_NAME = 0;

    private static final int PAGE_NUMBER = 1;

    private final MenuRepository menuRepository;

    private final UserService userService;

    private final KeyboardUtil keyboardUtil;

    private final TelegramBot bot;

    @Override
    @NonNull
    public Message initiateMenu(@NonNull String menuName, @NonNull User user) {
        return initiateMenu(menuName, userService.getUser(user.getId()), "");
    }

    @Override
    @NonNull
    public Message initiateMenu(@NonNull String menuName, @NonNull UserEntity user) {
        return initiateMenu(menuName, user, "");
    }

    @Override
    @NonNull
    public Message initiateMenu(@NonNull String menuName, @NonNull User user,
            @NonNull String param) {
        return initiateMenu(menuName, userService.getUser(user.getId()), param);
    }

    @Override
    @NonNull
    public Message initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull String param) {
        final Menu menu = menuRepository.find(menuName).orElseThrow(() ->
                new EntityNotFoundException("Menu " + menuName + " was not found"));
        if (menu.isAttachedToMessage()) {
            throw new UnsupportedOperationException("Menu " + menuName + " is supposed to be "
                    + "attached to a message, but the wrong initialization method was called.");
        }
        final Page firstPage = menu.getPages().get(0);
        final Localization localization = firstPage.getLocalizationFunction()
                .apply(user, List.of(param));
        
        LOGGER.info("Menu " + menuName + "'s message is being compiled for user "
                + user.getId() + "...");
        SendMessage sendMessage = SendMessage.builder()
                .chatId(user.getId())
                .text(localization.getData())
                .entities(localization.getEntities())
                .replyMarkup(getInitialMarkup(firstPage, param, user))
                .build();
        LOGGER.info("Menu " + menuName + "'s message compiled. Sending...");
        final Message message = bot.sendMessage(sendMessage);
        LOGGER.info("Message sent.");
        return message;
    }

    @Override
    public void initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull Integer messageId) {
        initiateMenu(menuName, user, "", messageId);
    }

    @Override
    public void initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull String param, @NonNull Integer messageId) {
        final Menu menu = menuRepository.find(menuName).orElseThrow(() ->
                new EntityNotFoundException("Menu " + menuName + " was not found"));
        final Page firstPage = menu.getPages().get(0);
        
        LOGGER.info("Menu " + menuName + "'s markup is being compiled for message " + messageId
                + " and user " + user.getId() + "...");
        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder()
                .chatId(user.getId())
                .messageId(messageId)
                .replyMarkup(getInitialMarkup(firstPage, param, user))
                .build();
        LOGGER.info("Menu " + menuName + "'s markup compiled. Sending...");
        try {
            bot.execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to update markup for message " + messageId
                    + " for user " + user.getId(), e);
        }
        LOGGER.info("Markup sent.");
    }

    @Override
    public void processCallbackQuery(@NonNull CallbackQuery query) {
        LOGGER.info("Menu callback detected. Button: " + query.getData() + ", User: "
                + query.getFrom().getId() + ".");
        final User user = query.getFrom();
        final UserEntity userFromDb = userService.getUser(user.getId());
        final String[] data = query.getData().split(DIVIDER);
        final Menu menu = menuRepository.find(data[MENU_NAME]).orElseThrow(() ->
                new EntityNotFoundException("Menu " + data[MENU_NAME] + " was not found"));
        
        Page page = menu.getPages().get(Integer.parseInt(data[PAGE_NUMBER]));
        LOGGER.info("Current page is " + data[PAGE_NUMBER] + ".");
        
        boolean hasMessageChanged = false;
        final EditMessageTextBuilder editMessageBuilder = EditMessageText.builder()
                .chatId(user.getId())
                .messageId(query.getMessage().getMessageId());
        final EditMessageReplyMarkupBuilder editMessageReplyMarkupBuilder = EditMessageReplyMarkup
                .builder()
                .chatId(user.getId())
                .messageId(query.getMessage().getMessageId());
            
        final Localization localization;
        final Button button = page.getButtonByData(userFromDb, data[data.length - 1]);
        switch (button.getType()) {
            case Button.Type.TRANSITORY:
                LOGGER.info("Button " + button.getData() + " is transitory.");
                final TransitoryButton transitoryButton = (TransitoryButton)button;
                LOGGER.info("Button parsed to transitory button. Next page will be "
                        + transitoryButton.getPagePointer() + ".");
                final Page nextPage = menu.getPages().get(transitoryButton.getPagePointer());
                final InlineKeyboardMarkup markup = getTransitoryMarkup(nextPage,
                            Arrays.copyOfRange(data, 2, data.length), userFromDb);
                LOGGER.info("Markup for page " + nextPage.getPageIndex() + " created.");

                if (menu.isAttachedToMessage()) {
                    editMessageReplyMarkupBuilder.replyMarkup(markup);
                } else {
                    localization = nextPage.getLocalizationFunction().apply(
                            userFromDb, Arrays.asList(Arrays.copyOfRange(data, 2, data.length)));

                    editMessageBuilder.replyMarkup(markup);
                    editMessageBuilder.text(localization.getData());
                    editMessageBuilder.entities(localization.getEntities());
                    LOGGER.info("Page requires its own content.");
                }
                hasMessageChanged = true;
                break;
            default:
                LOGGER.info("Button " + button.getData() + " is terminal.");
                if (menu.isOneTimeMenu()) {
                    final InlineKeyboardMarkup clearMarkup = InlineKeyboardMarkup.builder()
                            .clearKeyboard()
                            .keyboard(List.of())
                            .build();
                    if (menu.isAttachedToMessage()) {
                        editMessageReplyMarkupBuilder.replyMarkup(clearMarkup);
                    } else {
                        localization = menu.getPages().get(menu.getPages().size() - 1)
                                .getLocalizationFunction().apply(userFromDb,
                                (menu.isInitialParameterPresent()) ? List.of(data[2])
                                : List.of());
                        editMessageBuilder.text(localization.getData());
                        editMessageBuilder.entities(localization.getEntities());
                        editMessageBuilder.replyMarkup(clearMarkup);
                    }
                    hasMessageChanged = true;
                } else {
                    if (Integer.parseInt(data[PAGE_NUMBER]) != 0) {
                        final InlineKeyboardMarkup page0Markup = getInitialMarkup(
                                menu.getPages().get(0), (menu.isInitialParameterPresent())
                                ? data[2] : "", userFromDb);
                        if (menu.isAttachedToMessage()) {
                            editMessageReplyMarkupBuilder.replyMarkup(page0Markup);
                        } else {
                            localization = menu.getPages().get(0)
                                .getLocalizationFunction().apply(userFromDb,
                                (menu.isInitialParameterPresent()) ? List.of(data[2])
                                : List.of());
                            editMessageBuilder.text(localization.getData());
                            editMessageBuilder.entities(localization.getEntities());
                            editMessageBuilder.replyMarkup(page0Markup);
                        }             
                        hasMessageChanged = true;
                    }
                }
                
                final TerminalButton terminalButton = (TerminalButton)button;
                LOGGER.info("Button parsed to terminal button. Activating handler...");
                terminalButton.getHandler().handle(Arrays.copyOfRange(data, 2, data.length),
                        user);
                break;
        }
        AnswerCallbackQuery answerCallbackQuery = AnswerCallbackQuery.builder()
                .callbackQueryId(query.getId())
                .build();

        try {
            if (hasMessageChanged && (button.getType().equals(Button.Type.TERMINAL)
                    && menu.isUpdateAfterTerminalButtonRequired()
                    || button.getType().equals(Button.Type.TRANSITORY))) {
                if (menu.isAttachedToMessage()) {
                    LOGGER.info("Sending new message markup...");
                    bot.execute(editMessageReplyMarkupBuilder.build());
                    LOGGER.info("New markup sent.");
                } else {
                    LOGGER.info("Sending new message content...");
                    bot.execute(editMessageBuilder.build());
                    LOGGER.info("New content sent.");
                }  
            }
            
            LOGGER.info("Sending answer to callback query...");
            bot.execute(answerCallbackQuery);
            LOGGER.info("Answer sent.");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to update markup for message "
                    + query.getMessage().getMessageId() + " and user "
                    + user.getId(), e);
        }
    }

    @Override
    @NonNull
    public Menu save(@NonNull Menu menu) {
        return menuRepository.save(menu);
    }

    @Override
    public void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId,
            Localization terminalPageLocalization) {
        final InlineKeyboardMarkup clearMarkup = InlineKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of())
                .build();
        try {
            if (terminalPageLocalization == null) {
                bot.execute(EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(clearMarkup)
                        .build());
                return;
            }
            bot.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(terminalPageLocalization.getData())
                    .entities(terminalPageLocalization.getEntities())
                    .replyMarkup(clearMarkup)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to update message "
                    + messageId + " in chat " + chatId, e);
        }
    }

    private InlineKeyboardMarkup getInitialMarkup(Page menuPage, String param, UserEntity user) {
        final String callbackData = menuPage.getMenu().getName() + DIVIDER
                + menuPage.getPageIndex() + DIVIDER + ((param == "") ? param : param + DIVIDER);

        List<InlineKeyboardButton> buttons = menuPage.getButtonsFunction()
                .apply(user)
                .stream()
                .map(b -> InlineKeyboardButton.builder()
                    .callbackData(callbackData + b.getData())
                    .text(b.getName())
                    .build())
                .toList();
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardUtil.getInlineKeyboard(buttons))
                .build();
    }

    private InlineKeyboardMarkup getTransitoryMarkup(Page menuPage, String[] data,
            UserEntity user) {
        final StringBuilder builder = new StringBuilder().append(menuPage.getMenu().getName())
                .append(DIVIDER).append(menuPage.getPageIndex()).append(DIVIDER);
        for (String param : data) {
            builder.append(param).append(DIVIDER);
        }
        final String callbackData = builder.toString();
        
        List<InlineKeyboardButton> buttons = menuPage.getButtonsFunction()
                .apply(user)
                .stream()
                .map(b -> InlineKeyboardButton.builder()
                    .callbackData(callbackData + b.getData())
                    .text(b.getName())
                    .build())
                .toList();
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardUtil.getInlineKeyboard(buttons))
                .build();
    }
}

package com.unbidden.telegramcoursesbot.service.button.menu;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.util.KeyboardUtil;
import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText.EditMessageTextBuilder;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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

    private final KeyboardUtil keyboardUtil;

    private final TelegramBot bot;

    @Override
    public void initiateMenu(String menuName, User user) {
        final Menu menu = menuRepository.find(menuName).orElseThrow(() ->
                new EntityNotFoundException("Menu " + menuName + " was not found"));
        final Page firstPage = menu.getPages().get(0);
        
        LOGGER.info("Menu " + menuName + "'s message' is being compiled for user "
                + user.getId() + "...");
        SendMessage sendMessage = SendMessage.builder()
                    .chatId(user.getId())
                    .text(firstPage.getTextFunction().apply(user))
                    .replyMarkup(getMarkup0(firstPage, "", user))
                    .build();
        LOGGER.info("Menu " + menuName + "'s message compiled. Sending...");
        bot.sendMessage(sendMessage);
        LOGGER.info("Message sent.");
    }

    @Override
    public void processCallbackQuery(CallbackQuery query) {
        LOGGER.info("Menu callback detected. Button: " + query.getData() + ", User: "
                + query.getFrom().getId() + ".");
        final User user = query.getFrom();
        final String[] data = query.getData().split(DIVIDER);
        final Menu menu = menuRepository.find(data[MENU_NAME]).orElseThrow(() ->
                new EntityNotFoundException("Menu " + data[MENU_NAME] + " was not found"));
        
        Page page = menu.getPages().get(Integer.parseInt(data[PAGE_NUMBER]));
        LOGGER.info("Current page is " + data[PAGE_NUMBER] + ".");
        
        EditMessageTextBuilder editMessageBuilder = EditMessageText.builder()
                .chatId(user.getId())
                .messageId(query.getMessage().getMessageId());
                
        switch (page.getType()) {
            case Page.Type.TRANSITORY:
                LOGGER.info("Page " + data[PAGE_NUMBER] + " is transitory.");
                Page nextPage = menu.getPages().get(Integer.parseInt(data[PAGE_NUMBER] + 1));

                editMessageBuilder.replyMarkup(getMarkup0(nextPage, data[data.length - 1], user));
                editMessageBuilder.text(nextPage.getTextFunction().apply(user));
                LOGGER.info("Markup for page " + (Integer.parseInt(data[PAGE_NUMBER] + 1))
                        + " created.");
                break;
            default:
                LOGGER.info("Page " + data[PAGE_NUMBER] + " is terminal.");
                editMessageBuilder.replyMarkup(getMarkup0(menu.getPages().get(0), "", user));
                editMessageBuilder.text(menu.getPages().get(0).getTextFunction().apply(user));
                
                TerminalButton button = (TerminalButton)page.getButtonByData(user,
                        data[data.length - 1]);
                LOGGER.info("Button parsed to terminal button. Activating handler...");
                button.getHandler().handle(Arrays.copyOfRange(data, 2, data.length - 1), user);
                break;
        }
        AnswerCallbackQuery answerCallbackQuery = AnswerCallbackQuery.builder()
                .callbackQueryId(query.getId())
                .build();

        try {
            LOGGER.info("Sending answer to callback query...");
            bot.execute(answerCallbackQuery);
            LOGGER.info("Answer sent. Sending new message content...");
            bot.execute(editMessageBuilder.build());
            LOGGER.info("New content sent.");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to update markup for message "
                    + query.getMessage().getMessageId() + " and user "
                    + user.getId(), e);
        }
    }

    private InlineKeyboardMarkup getMarkup0(Page menuPage, String param, User user) {
        final String initialParam = menuPage.getMenu().getName() + DIVIDER
                + menuPage.getPageIndex() + DIVIDER;

        List<InlineKeyboardButton> buttons = menuPage.getButtonsFunction()
                .apply(user)
                .stream()
                .map(b -> InlineKeyboardButton.builder()
                    .callbackData(initialParam + ((param == "") ? param : param + DIVIDER)
                        + b.getData())
                    .text(b.getName())
                    .build())
                .toList();
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardUtil.getInlineKeyboard(buttons))
                .build();
    }
}

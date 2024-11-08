package com.unbidden.telegramcoursesbot.service.button.menu;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.MenuTerminationGroup;
import com.unbidden.telegramcoursesbot.model.MessageEntity;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.CallbackQueryRepository;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.repository.MenuTerminationGroupRepository;
import com.unbidden.telegramcoursesbot.repository.MessageRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.KeyboardUtil;
import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup.EditMessageReplyMarkupBuilder;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText.EditMessageTextBuilder;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {
    private static final Logger LOGGER = LogManager.getLogger(MenuServiceImpl.class);

    private static final String ERROR_UPDATE_MESSAGE_FAILURE = "error_update_message_failure";
    private static final String ERROR_UPDATE_MARKUP_FAILURE = "error_update_markup_failure";
    
    private static final String DIVIDER = ":";
    private static final int MENU_NAME = 0;
    private static final int PAGE_NUMBER = 1;

    private final MenuRepository menuRepository;

    private final MenuTerminationGroupRepository menuTerminationGroupRepository;

    private final MessageRepository messageRepository;

    private final CallbackQueryRepository callbackQueryRepository;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final KeyboardUtil keyboardUtil;

    private final CustomTelegramClient client;

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

        LOGGER.debug("Sending menu " + menu.getName() + " to user " + user.getId() + "...");
        final Message message = client.sendMessage(user, localization,
                getInitialMarkup(firstPage, param, user));
        LOGGER.debug("Message sent.");
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
        
        LOGGER.debug("Menu " + menuName + "'s markup is being compiled for message " + messageId
                + " and user " + user.getId() + "...");
        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder()
                .chatId(user.getId())
                .messageId(messageId)
                .replyMarkup(getInitialMarkup(firstPage, param, user))
                .build();
        LOGGER.debug("Menu " + menuName + "'s markup compiled. Sending...");
        try {
            client.execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to update markup for message " + messageId
                    + " for user " + user.getId(), localizationLoader.getLocalizationForUser(
                    ERROR_UPDATE_MARKUP_FAILURE, user), e);
        }
        LOGGER.debug("Markup sent.");
    }

    @Override
    public void processCallbackQuery(@NonNull CallbackQuery query) {
        final User user = query.getFrom();
        final UserEntity userFromDb = userService.getUser(user.getId());
        final String[] data = query.getData().split(DIVIDER);
        final Menu menu = menuRepository.find(data[MENU_NAME]).orElseThrow(() ->
                new EntityNotFoundException("Menu " + data[MENU_NAME] + " was not found"));
        
        LOGGER.debug("Saving callback querry...");
        callbackQueryRepository.save(query);
        LOGGER.debug("Callback query saved.");
        Page page = menu.getPages().get(Integer.parseInt(data[PAGE_NUMBER]));
        LOGGER.debug("Current page is " + data[PAGE_NUMBER] + ".");
        
        boolean hasMessageChanged = false;
        final EditMessageTextBuilder<?, ?> editMessageBuilder = EditMessageText.builder()
                .chatId(user.getId())
                .messageId(query.getMessage().getMessageId());
        final EditMessageReplyMarkupBuilder editMessageReplyMarkupBuilder = EditMessageReplyMarkup
                .builder()
                .chatId(user.getId())
                .messageId(query.getMessage().getMessageId());
            
        final Localization localization;
        final String[] paramsOnly = Arrays.copyOfRange(data, 2, data.length);
        final Button button = page.getButtonByData(userFromDb, data[data.length - 1], paramsOnly);
        switch (button.getType()) {
            case Button.Type.TRANSITORY:
                LOGGER.debug("Button " + button.getData() + " is transitory.");
                final TransitoryButton transitoryButton = (TransitoryButton)button;
                LOGGER.debug("Button parsed to transitory button. Next page will be "
                        + transitoryButton.getPagePointer() + ".");
                final Page nextPage = menu.getPages().get(transitoryButton.getPagePointer());
                final InlineKeyboardMarkup markup = getTransitoryMarkup(nextPage,
                        paramsOnly, userFromDb);
                LOGGER.debug("Markup for page " + nextPage.getPageIndex() + " created.");

                if (menu.isAttachedToMessage()) {
                    editMessageReplyMarkupBuilder.replyMarkup(markup);
                } else {
                    localization = nextPage.getLocalizationFunction().apply(
                            userFromDb, Arrays.asList(paramsOnly));

                    editMessageBuilder.replyMarkup(markup);
                    editMessageBuilder.text(localization.getData());
                    editMessageBuilder.entities(localization.getEntities());
                    LOGGER.debug("Page requires its own content.");
                }
                hasMessageChanged = true;
                break;
            default:
                LOGGER.debug("Button " + button.getData() + " is terminal.");
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
                LOGGER.debug("Button parsed to terminal button. Activating handler...");
                terminalButton.getHandler().handle(userService.getUser(user.getId()), paramsOnly);
                break;
        }

        try {
            if (hasMessageChanged && (button.getType().equals(Button.Type.TERMINAL)
                    && menu.isUpdateAfterTerminalButtonRequired()
                    || button.getType().equals(Button.Type.TRANSITORY))) {
                if (menu.isAttachedToMessage()) {
                    LOGGER.debug("Sending new message markup...");
                    client.execute(editMessageReplyMarkupBuilder.build());
                    LOGGER.debug("New markup sent.");
                } else {
                    LOGGER.debug("Sending new message content...");
                    client.execute(editMessageBuilder.build());
                    LOGGER.debug("New content sent.");
                }  
            }
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to update markup for message "
                    + query.getMessage().getMessageId() + " and user "
                    + user.getId(), localizationLoader.getLocalizationForUser(
                    ERROR_UPDATE_MARKUP_FAILURE, userFromDb), e);
        }
    }

    @Override
    @NonNull
    public Menu save(@NonNull Menu menu) {
        return menuRepository.save(menu);
    }

    @Override
    @NonNull
    public MenuTerminationGroup addToMenuTerminationGroup(@NonNull UserEntity user,
            @NonNull UserEntity messagedUser, @NonNull Integer messageId, @NonNull String key,
            @Nullable String terminalLocalizationName) {
        final Optional<MenuTerminationGroup> groupOpt = menuTerminationGroupRepository
                .findByUserIdAndName(user.getId(), key);
        final MenuTerminationGroup group;
        if (groupOpt.isPresent()) {
            LOGGER.debug("MTG for user " + user.getId() + " and key " + key + " already exists.");
            group = groupOpt.get();
            
            group.getMessages().add(messageRepository.save(new MessageEntity(messagedUser,
                    messageId)));
        } else {
            LOGGER.debug("MTG " + user.getId() + " and key " + key + " does not exist yet.");
            group = new MenuTerminationGroup();
            group.setName(key);
            group.setMessages(List.of(messageRepository.save(
                    new MessageEntity(messagedUser, messageId))));
            group.setTerminalLocalizationName(terminalLocalizationName);
            group.setUser(user);
        }
        LOGGER.debug("Persisting or updating the MTG...");
        menuTerminationGroupRepository.save(group);
        LOGGER.debug("Operation successful.");
        return group;
    }

    @Override
    public void terminateMenuGroup(@NonNull UserEntity user, @NonNull String key) {
        final MenuTerminationGroup group = menuTerminationGroupRepository.findByUserIdAndName(
                user.getId(), key).orElseThrow(() -> new EntityNotFoundException
                ("Menu termination group for user " + user.getId() + " and key " + key
                + " does not exist."));
        
        for (MessageEntity message : group.getMessages()) {
            terminateMenu(message.getUser().getId(), message.getMessageId(),
                    (group.getTerminalLocalizationName() != null) ? localizationLoader
                    .getLocalizationForUser(group.getTerminalLocalizationName(),
                    message.getUser()) : null);
        }
        menuTerminationGroupRepository.delete(group);
    }

    @Override
    public void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId,
            @Nullable Localization terminalPageLocalization) {
        final UserEntity user = userService.getUser(chatId);
        final InlineKeyboardMarkup clearMarkup = InlineKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of())
                .build();
        try {
            if (terminalPageLocalization == null) {
                client.execute(EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(clearMarkup)
                        .build());
                return;
            }
            client.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(terminalPageLocalization.getData())
                    .entities(terminalPageLocalization.getEntities())
                    .replyMarkup(clearMarkup)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to update message "
                    + messageId + " in chat " + chatId, localizationLoader.getLocalizationForUser(
                    ERROR_UPDATE_MESSAGE_FAILURE, user), e);
        }
    }

    @Override
    public void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId) {
        terminateMenu(chatId, messageId, null);
    }

    private InlineKeyboardMarkup getInitialMarkup(Page menuPage, String param, UserEntity user) {
        final String callbackData = menuPage.getMenu().getName() + DIVIDER
                + menuPage.getPageIndex() + DIVIDER + ((param == "") ? param : param + DIVIDER);
        List<InlineKeyboardButton> buttons = menuPage.getButtonsFunction()
                .apply(user, List.of(param))
                .stream()
                .map(b -> (InlineKeyboardButton)InlineKeyboardButton.builder()
                    .callbackData(callbackData + b.getData())
                    .text(b.getName())
                    .build())
                .toList();
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardUtil.getInlineKeyboard(buttons, menuPage.getButtonsRowSize()))
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
                .apply(user, Arrays.asList(data))
                .stream()
                .map(b -> (InlineKeyboardButton)InlineKeyboardButton.builder()
                    .callbackData(callbackData + b.getData())
                    .text(b.getName())
                    .build())
                .toList();
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardUtil.getInlineKeyboard(buttons, menuPage.getButtonsRowSize()))
                .build();
    }
}

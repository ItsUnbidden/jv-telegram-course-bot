package com.unbidden.telegramcoursesbot.service.menu;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.MenuExpiredException;
import com.unbidden.telegramcoursesbot.exception.NoDataForMultipageListException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.LocalizationKey;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.MenuTerminationGroup;
import com.unbidden.telegramcoursesbot.model.MessageEntity;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.CallbackQueryRepository;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.repository.MultipageListMetaRepository;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.LinkButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button.Type;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.KeyboardUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup.EditMessageReplyMarkupBuilder;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText.EditMessageTextBuilder;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@RequiredArgsConstructor
public class MenuService {
    private static final Logger LOGGER = LogManager.getLogger(MenuService.class);

    private static final String DIVIDER = ":";
    private static final int MENU_NAME = 0;
    private static final int PAGE_NUMBER = 1;
    private static final int NUMBER_OF_ELEMENTS_PER_PAGE_ON_MULTIPAGE_LIST = 15;

    private final MenuTerminationGroupService menuTerminationGroupService;

    private final MenuRepository menuRepository;

    private final MultipageListMetaRepository multipageListMetaRepository;

    private final CallbackQueryRepository callbackQueryRepository;

    private final LocalizationLoader localizationLoader;

    private final KeyboardUtil keyboardUtil;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    public Message initiateMenu(UserEntity user, Bot bot, MenuKey key) {
        return initiateMenu(user, bot, key, "");
    }

    public Message initiateMenu(UserEntity user, Bot bot, MenuKey key, String param) {
        final Menu menu = menuRepository.find(key.getName()).orElseThrow(() ->
                new EntityNotFoundException("Menu " + key.getName() + " was not found",
                localizationLoader.getLocalizationForUser(Error.MENU_NOT_FOUND, user)));
        if (menu.isAttachedToMessage()) {
            throw new UnsupportedOperationException("Menu " + key.getName() + " is supposed to be "
                    + "attached to a message, but the wrong initialization method was called.");
        }
        final Page firstPage = menu.getPages().get(0);
        final Localization localization = firstPage.getLocalizationFunction()
                .apply(user, List.of(param), bot);

        LOGGER.trace("Sending menu " + menu.getName() + " to user " + user.getId() + "...");
        final Message message = clientManager.getClient(bot).sendMessage(user, localization,
                getInitialMarkup(firstPage, param, user, bot));
        LOGGER.trace("Message sent.");
        return message;
    }

    public void initiateMenu(UserEntity user, Bot bot, MenuKey key, Integer messageId) {
        initiateMenu(user, bot, key, "", messageId);
    }

    public void initiateMenu(UserEntity user, Bot bot, MenuKey key, String param, Integer messageId) {
        final Menu menu = menuRepository.find(key.getName()).orElseThrow(() ->
                new EntityNotFoundException("Menu " + key.getName() + " was not found",
                localizationLoader.getLocalizationForUser(Error.MENU_NOT_FOUND, user)));
        final Page firstPage = menu.getPages().get(0);
        
        LOGGER.trace("Menu " + key + "'s markup is being compiled for message " + messageId
                + " and user " + user.getId() + "...");
        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder()
                .chatId(user.getId())
                .messageId(messageId)
                .replyMarkup(getInitialMarkup(firstPage, param, user, bot))
                .build();
        LOGGER.trace("Menu " + key + "'s markup compiled. Sending...");
        try {
            clientManager.getClient(bot).execute(editMessageReplyMarkup);
            LOGGER.trace("Markup sent.");
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to update markup for message " + messageId + " for user "
                    + user.getId(), e);
        }
    }

    public void processCallbackQuery(CallbackQuery query, Bot bot) {
        final UserEntity user = entityUtil.getUser(query.getFrom());
        final String[] data = query.getData().split(DIVIDER);
        final Menu menu = menuRepository.find(data[MENU_NAME]).orElseThrow(() ->
                new EntityNotFoundException("Menu " + data[MENU_NAME] + " was not found",
                localizationLoader.getLocalizationForUser(Error.MENU_NOT_FOUND, user)));
                
        LOGGER.trace("Saving callback querry...");
        callbackQueryRepository.save(query, user, bot);
        LOGGER.trace("Callback query saved.");
        Page page = menu.getPages().get(Integer.parseInt(data[PAGE_NUMBER]));
        LOGGER.trace("Current page is " + data[PAGE_NUMBER] + ".");
        
        boolean hasMessageChanged = false;
        final EditMessageTextBuilder<?, ?> editMessageBuilder = EditMessageText.builder()
                .chatId(user.getId())
                .messageId(query.getMessage().getMessageId());
        final EditMessageReplyMarkupBuilder editMessageReplyMarkupBuilder = EditMessageReplyMarkup
                .builder()
                .chatId(user.getId())
                .messageId(query.getMessage().getMessageId());
            
        final String[] paramsOnly = Arrays.copyOfRange(data, 2, data.length);
        final Button button;
        try {
            button = page.getButtonByData(user, bot, data[data.length - 1], paramsOnly);
        } catch (MenuExpiredException e) {
            throw new ActionExpiredException("Menu has changed and user " + user.getId()
                    + "'s request is unprocessable", localizationLoader.getLocalizationForUser(
                    Error.MENU_BUTTON_ISSUE, user));
        }
        switch (button.getType()) {
            case Button.Type.TRANSITORY:
                LOGGER.trace("Button " + button.getData() + " is transitory.");
                final TransitoryButton transitoryButton = (TransitoryButton)button;

                LOGGER.trace("Button parsed to transitory button. Next page will be "
                        + transitoryButton.getPagePointer() + ".");
                final Page nextPage = menu.getPages().get(transitoryButton.getPagePointer());
                final InlineKeyboardMarkup markup = getTransitoryMarkup(nextPage,
                        paramsOnly, user, bot);

                LOGGER.trace("Markup for page " + nextPage.getPageIndex() + " created.");

                buildMessage(editMessageBuilder, editMessageReplyMarkupBuilder, markup,
                        (!menu.isAttachedToMessage()) ?
                        nextPage.getLocalizationFunction().apply(user, Arrays.asList(paramsOnly),
                        bot) : null, menu.isAttachedToMessage());
                hasMessageChanged = true;
                break;
            case Button.Type.BACKWARD:
                LOGGER.trace("Button " + button.getData() + " is backward.");
                if (page.getPageIndex() == 0) {
                    throw new UnsupportedOperationException("Backward button cannot "
                            + "be on the first page");
                }
                final Page previousPage = menu.getPages().get(page.getPreviousPage());
                LOGGER.trace("Returning to previous page number " + previousPage.getPageIndex());
                final String[] trimmedParams = Arrays.copyOfRange(paramsOnly, 0,
                        paramsOnly.length - 2);
                final InlineKeyboardMarkup previousMarkup = getTransitoryMarkup(previousPage,
                        trimmedParams, user, bot);
                LOGGER.trace("Markup for page " + previousPage.getPageIndex() + " created.");

                buildMessage(editMessageBuilder, editMessageReplyMarkupBuilder, previousMarkup,
                        (!menu.isAttachedToMessage()) ?
                        previousPage.getLocalizationFunction().apply(user,
                        Arrays.asList(trimmedParams), bot) : null, menu.isAttachedToMessage());
                hasMessageChanged = true;
                break;
            default:
                LOGGER.trace("Button " + button.getData() + " is terminal.");
                if (menu.isOneTimeMenu()) {
                    final InlineKeyboardMarkup clearMarkup = InlineKeyboardMarkup.builder()
                            .clearKeyboard()
                            .keyboard(List.of())
                            .build();
                    buildMessage(editMessageBuilder, editMessageReplyMarkupBuilder, clearMarkup,
                            (!menu.isAttachedToMessage()) ? 
                            menu.getPages().get(menu.getPages().size() - 1)
                            .getLocalizationFunction().apply(user,
                            (menu.isInitialParameterPresent()) ? List.of(data[2])
                            : List.of(), bot) : null, menu.isAttachedToMessage());
                    hasMessageChanged = true;
                } else {
                    if (Integer.parseInt(data[PAGE_NUMBER]) != 0) {
                        final InlineKeyboardMarkup page0Markup = getInitialMarkup(
                                menu.getPages().get(0), (menu.isInitialParameterPresent())
                                ? data[2] : "", user, bot);
                        buildMessage(editMessageBuilder, editMessageReplyMarkupBuilder,
                                page0Markup, (!menu.isAttachedToMessage()) ? menu.getPages()
                                .get(0).getLocalizationFunction().apply(user,
                                (menu.isInitialParameterPresent()) ? List.of(data[2])
                                : List.of(), bot) : null, menu.isAttachedToMessage());
                        hasMessageChanged = true;
                    }
                }
                
                final TerminalButton terminalButton = (TerminalButton)button;
                LOGGER.trace("Button parsed to terminal button. Activating handler...");
                terminalButton.getHandler().handle(bot, user, paramsOnly);
                break;
        }

        try {
            if (hasMessageChanged && (button.getType().equals(Button.Type.TERMINAL)
                    && menu.isUpdateAfterTerminalButtonRequired()
                    || !button.getType().equals(Button.Type.TERMINAL))) {
                if (menu.isAttachedToMessage()) {
                    LOGGER.trace("Sending new message markup...");
                    clientManager.getClient(bot).execute(editMessageReplyMarkupBuilder.build());
                    LOGGER.trace("New markup sent.");
                } else {
                    LOGGER.trace("Sending new message content...");
                    clientManager.getClient(bot).execute(editMessageBuilder.build());
                    LOGGER.trace("New content sent.");
                }  
            }
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to update markup for message " + query.getMessage()
                    .getMessageId() + " and user " + user.getId(), e);
        }
    }

    public Message initiateMultipageList(UserEntity user, Bot bot,
            Function<MultipageListParams, Localization> localizationFunction,
            BiFunction<Integer, Integer, org.springframework.data.domain.Page<String>> dataFunction) {
        LOGGER.debug("Initiating a new multipage list... Applying data function...");
        final org.springframework.data.domain.Page<String> dataPage = dataFunction.apply(0,
                NUMBER_OF_ELEMENTS_PER_PAGE_ON_MULTIPAGE_LIST);

        if (dataPage.getTotalElements() == 0) {
            throw new NoDataForMultipageListException("No data available for multipage list",
                    localizationLoader.getLocalizationForUser(Error.NO_DATA_FOR_MULTIPAGE_LIST,
                    user));
        }
        final String data = convertDataPageToString(dataPage);
        
        LOGGER.debug("Data function applied and data parsed. Sending the message...");
        final Message message = clientManager.getClient(bot).sendMessage(user,
                localizationFunction.apply(new MultipageListParams(0, dataPage.getTotalPages(),
                dataPage.getTotalElements(), data)));

        LOGGER.debug("Message has been sent.");

        if (dataPage.getTotalPages() > 1) {
            LOGGER.debug("There is more than one page. Creating new multipage meta...");
            final MultipageListMeta meta = new MultipageListMeta(UUID.randomUUID(), user, bot,
                    message.getMessageId(), 0, localizationFunction, dataFunction);

            meta.setNumberOfElements(dataPage.getTotalElements());
            meta.setNumberOfPages(dataPage.getTotalPages());
            multipageListMetaRepository.save(meta);
            LOGGER.debug("Multipage meta " + meta.getId() + " has been created and persisted.");

            LOGGER.debug("Attaching a control menu...");
            initiateMenu(user, bot, MenuKey.MULTIPAGE_LIST ,meta.getId().toString(), message.getMessageId());
            LOGGER.debug("Menu initiated.");
        }
        return message;
    }

    public void processMultipageListRequest(MultipageListMeta meta) {
        LOGGER.debug("Updating multipage list message " + meta.getMessageId() + " for user "
                + meta.getUser().getId() + "...");
        final Localization localization = meta.getLocalizationFunction().apply(
                new MultipageListParams(meta.getPage(), meta.getNumberOfPages(), meta.getNumberOfElements(),
                convertDataPageToString(meta.getDataFunction().apply(meta.getPage(), NUMBER_OF_ELEMENTS_PER_PAGE_ON_MULTIPAGE_LIST))));
        final Menu menu = menuRepository.find(MenuKey.MULTIPAGE_LIST.getName()).get();

        try {
            clientManager.getClient(meta.getBot()).execute(EditMessageText.builder()
                    .chatId(meta.getUser().getId())
                    .messageId(meta.getMessageId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .replyMarkup(getInitialMarkup(menu.getPages().getFirst(),
                        meta.getId().toString(), meta.getUser(), meta.getBot()))
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to update a multipage list message "
                    + meta.getMessageId() + " for user " + meta.getUser().getId(),
                    localizationLoader.getLocalizationForUser(Error.UPDATE_MESSAGE_FAILURE,
                    meta.getUser()), e);
        }
    }

    public MultipageListMeta getMultipageListMeta(UUID id, UserEntity user) {
        return multipageListMetaRepository.find(id).orElseThrow(() -> new EntityNotFoundException(
                "Multipage list meta " + id + " does not exist. It might have expired.",
                localizationLoader.getLocalizationForUser(Error.MULTIPAGE_LIST_META_NOT_FOUND,
                user)));
    }

    public Menu save(Menu menu) {
        return menuRepository.save(menu);
    }

    /**
     * Adds a menu to a new menu termination group. If the group doesn't exist, it is created.
     * @param user with whom the group is associated.
     * @param messagedUser — the user to whom the menu was sent.
     * @param bot
     * @param messageId 
     * @param key that identifies this group.
     * @param terminalLocalizationKey — the key of the localization that will be used to replace the message's text after the menu is removed.
     * @param args that will be used to format the MTG key.
     * @return
     */
    public MenuTerminationGroup addToMenuTerminationGroup(UserEntity user,
            UserEntity messagedUser, Bot bot, Integer messageId,
            MenuTerminationGroupKey key, @Nullable LocalizationKey terminalLocalizationKey, Object... args) {
        return menuTerminationGroupService.addToMenuTerminationGroup(user, messagedUser,
                bot, messageId, key, terminalLocalizationKey, args);
    }

    /**
     * Adds a menu to a new menu termination group. If the group doesn't exist, it is created. There will be no terminal localization.
     * @param user with whom the group is associated.
     * @param messagedUser — the user to whom the menu was sent.
     * @param bot
     * @param messageId
     * @param key that identifies this group.
     * @param args that will be used to format the MTG key.
     * @return
     */
    public MenuTerminationGroup addToMenuTerminationGroup(UserEntity user, UserEntity messagedUser,
            Bot bot, Integer messageId, MenuTerminationGroupKey key, Object... args) {
        return addToMenuTerminationGroup(user, messagedUser, bot, messageId, key, null, args);
    }

    /**
     * Removes the menus for all messages in the specified menu termination group. If a menu cannot be removed, the exception will be ignored.
     * @param user with whom the group is associated.
     * @param bot
     * @param key that identifies this group.
     * @param args that will be used to format the MTG key.
     */
    public void terminateMenuGroup(UserEntity user, Bot bot, MenuTerminationGroupKey key, Object... args) {
        terminateMenuGroup(user, bot, key, null, args);
    }

    /**
     * Removes the menus for all messages in the specified menu termination group. If a menu cannot be removed, the exception will be ignored.
     * @param user with whom the group is associated.
     * @param bot
     * @param key that identifies this group.
     * @param terminalLocalizationOverride — the key of the localization that will be used to replace the message's text after the menu is removed. If the MTG contained a {@code terminalLocalizationKey}, it will be overridden.
     * @param args that will be used to format the MTG key.
     */
    public void terminateMenuGroup(UserEntity user, Bot bot, MenuTerminationGroupKey key,
            @Nullable Localization terminalLocalizationOverride, Object... args) {
        final MenuTerminationGroup group = menuTerminationGroupService.terminateMenuGroup(user, key, args);
            
        for (final MessageEntity message : group.getMessages()) {
            terminateMenu(message.getUser().getId(), message.getMessageId(), bot,
                    (terminalLocalizationOverride != null) ? terminalLocalizationOverride
                    : (group.getTerminalLocalizationName() != null) ? localizationLoader
                    .getLocalizationForUser(Localizations.getKeyByLocName(group.getTerminalLocalizationName()),
                    message.getUser()) : null);
        }
    }

    /**
     * Removes a specific menu. 
     * @param chatId 
     * @param messageId
     * @param bot
     * @param terminalPageLocalization — the localization that will be used to replace the text of the removed menu's message.
     */
    public void terminateMenu(Long chatId, Integer messageId, Bot bot,
            @Nullable Localization terminalPageLocalization) {
        final InlineKeyboardMarkup clearMarkup = InlineKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of())
                .build();
        try {
            if (terminalPageLocalization == null) {
                clientManager.getClient(bot).execute(EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(clearMarkup)
                        .build());
                return;
            }
            clientManager.getClient(bot).execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(terminalPageLocalization.getData())
                    .entities(terminalPageLocalization.getEntities())
                    .replyMarkup(clearMarkup)
                    .build());
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to update message " + messageId + " in chat " + chatId, e);
            // TODO: make sure ignoring this does not cause any issues
        }
    }
    
    /**
     * Removes a specific menu. 
     * @param chatId 
     * @param messageId
     * @param bot
     */
    public void terminateMenu(Long chatId, Integer messageId, Bot bot) {
        terminateMenu(chatId, messageId, bot, null);
    }

    public void answerPotentialCallbackQuery(UserEntity user, Bot bot)
            throws CallbackQueryAnswerException {
        final Optional<CallbackQuery> query = callbackQueryRepository.findAndRemove(user, bot);

        if (query.isPresent()) {
            LOGGER.debug("User " + user.getId() + " has an unanswered callback query.");
            try {
                clientManager.getClient(bot).execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(query.get().getId())
                        .build());
                LOGGER.debug("Callback query resolved.");
            } catch (TelegramApiException e) {
                throw new CallbackQueryAnswerException("Unable to answer callback query", e);
            }
        }
    }

    private void buildMessage(EditMessageTextBuilder<?, ?> editMessageBuilder,
            EditMessageReplyMarkupBuilder editMarkupBuilder, InlineKeyboardMarkup markup,
            Localization localization, boolean isAttachedToMessage) {
        if (isAttachedToMessage) {
            editMarkupBuilder.replyMarkup(markup);
        } else {
            editMessageBuilder.replyMarkup(markup);
            editMessageBuilder.text(localization.getData());
            editMessageBuilder.entities(localization.getEntities());
            LOGGER.trace("Page requires its own content.");
        }
    }

    private InlineKeyboardMarkup getInitialMarkup(Page menuPage, String param,
            UserEntity user, Bot bot) {
        final String callbackData = menuPage.getMenu().getName() + DIVIDER
                + menuPage.getPageIndex() + DIVIDER + ((param == "") ? param : param + DIVIDER);
        List<InlineKeyboardButton> buttons = menuPage.getButtonsFunction()
                .apply(user, List.of(param), bot)
                .stream()
                .map(b -> (InlineKeyboardButton)InlineKeyboardButton.builder()
                    .callbackData(b.getType() != Type.LINK ? callbackData + b.getData() : null)
                    .url(b.getType() == Type.LINK ? ((LinkButton)b).getUrl() : null)
                    .text(b.getName())
                    .build()
                )
                .toList();
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardUtil.getInlineKeyboard(buttons, menuPage.getButtonsRowSize()))
                .build();
    }

    private InlineKeyboardMarkup getTransitoryMarkup(Page menuPage, String[] data,
            UserEntity user, Bot bot) {
        final StringBuilder builder = new StringBuilder().append(menuPage.getMenu().getName())
                .append(DIVIDER).append(menuPage.getPageIndex()).append(DIVIDER);
        for (String param : data) {
            builder.append(param).append(DIVIDER);
        }
        final String callbackData = builder.toString();
        
        List<InlineKeyboardButton> buttons = menuPage.getButtonsFunction()
                .apply(user, Arrays.asList(data), bot)
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

    private String convertDataPageToString(org.springframework.data.domain.Page<String> dataPage) {
        final StringBuilder builder = new StringBuilder();

        for (String entry : dataPage.getContent()) {
            builder.append(entry).append('\n').append("-----").append('\n');
        }
        builder.delete(builder.length() - 7, builder.length());
        
        return builder.toString();
    }
}

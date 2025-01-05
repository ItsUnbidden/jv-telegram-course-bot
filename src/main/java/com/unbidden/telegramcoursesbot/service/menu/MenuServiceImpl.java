package com.unbidden.telegramcoursesbot.service.menu;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.MenuExpiredException;
import com.unbidden.telegramcoursesbot.exception.NoDataForMultipageListException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.MenuTerminationGroup;
import com.unbidden.telegramcoursesbot.model.MessageEntity;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.CallbackQueryRepository;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.repository.MenuTerminationGroupRepository;
import com.unbidden.telegramcoursesbot.repository.MessageRepository;
import com.unbidden.telegramcoursesbot.repository.MultipageListMetaRepository;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.KeyboardUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
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
public class MenuServiceImpl implements MenuService {
    private static final Logger LOGGER = LogManager.getLogger(MenuServiceImpl.class);

    private static final String PARAM_CURRENT_PAGE = "${currentPage}";
    private static final String PARAM_NUMBER_OF_PAGES = "${numberOfPages}";
    private static final String PARAM_DATA = "${data}";
    
    private static final String ERROR_UPDATE_MESSAGE_FAILURE = "error_update_message_failure";
    private static final String ERROR_MTG_NOT_FOUND = "error_mtg_not_found";
    private static final String ERROR_MENU_NOT_FOUND = "error_menu_not_found";
    private static final String ERROR_MENU_BUTTON_ISSUE = "error_menu_button_issue";
    private static final String ERROR_MULTIPAGE_LIST_META_NOT_FOUND =
            "error_multipage_list_meta_not_found";
    private static final String ERROR_NO_DATA_FOR_MULTIPAGE_LIST =
            "error_no_data_for_multipage_list";
    
    private static final String DIVIDER = ":";
    private static final int MENU_NAME = 0;
    private static final int PAGE_NUMBER = 1;
    private static final int NUMBER_OF_ELEMENTS_PER_PAGE_ON_MULTIPAGE_LIST = 1;
    private static final String MULTIPAGE_MENU_NAME = "m_mpl";

    private final MenuRepository menuRepository;

    private final MenuTerminationGroupRepository menuTerminationGroupRepository;

    private final MultipageListMetaRepository multipageListMetaRepository;

    private final MessageRepository messageRepository;

    private final CallbackQueryRepository callbackQueryRepository;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final KeyboardUtil keyboardUtil;

    private final ClientManager clientManager;

    @Override
    @NonNull
    public Message initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull Bot bot) {
        return initiateMenu(menuName, user, "", bot);
    }

    @Override
    @NonNull
    public Message initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull String param, @NonNull Bot bot) {
        final Menu menu = menuRepository.find(menuName).orElseThrow(() ->
                new EntityNotFoundException("Menu " + menuName + " was not found",
                localizationLoader.getLocalizationForUser(ERROR_MENU_NOT_FOUND, user)));
        if (menu.isAttachedToMessage()) {
            throw new UnsupportedOperationException("Menu " + menuName + " is supposed to be "
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

    @Override
    public void initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull Integer messageId, @NonNull Bot bot) {
        initiateMenu(menuName, user, "", messageId, bot);
    }

    @Override
    public void initiateMenu(@NonNull String menuName, @NonNull UserEntity user,
            @NonNull String param, @NonNull Integer messageId, @NonNull Bot bot) {
        final Menu menu = menuRepository.find(menuName).orElseThrow(() ->
                new EntityNotFoundException("Menu " + menuName + " was not found",
                localizationLoader.getLocalizationForUser(ERROR_MENU_NOT_FOUND, user)));
        final Page firstPage = menu.getPages().get(0);
        
        LOGGER.trace("Menu " + menuName + "'s markup is being compiled for message " + messageId
                + " and user " + user.getId() + "...");
        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder()
                .chatId(user.getId())
                .messageId(messageId)
                .replyMarkup(getInitialMarkup(firstPage, param, user, bot))
                .build();
        LOGGER.trace("Menu " + menuName + "'s markup compiled. Sending...");
        try {
            clientManager.getClient(bot).execute(editMessageReplyMarkup);
            LOGGER.trace("Markup sent.");
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to update markup for message " + messageId + " for user "
                    + user.getId(), e);
        }
    }

    @Override
    public void processCallbackQuery(@NonNull CallbackQuery query, @NonNull Bot bot) {
        final UserEntity user = userService.getUser(query.getFrom().getId(),
                userService.getDiretor());
        final String[] data = query.getData().split(DIVIDER);
        final Menu menu = menuRepository.find(data[MENU_NAME]).orElseThrow(() ->
                new EntityNotFoundException("Menu " + data[MENU_NAME] + " was not found",
                localizationLoader.getLocalizationForUser(ERROR_MENU_NOT_FOUND, user)));
                
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
                    ERROR_MENU_BUTTON_ISSUE, user));
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

    @Override
    @NonNull
    public Message initiateMultipageList(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull Function<Map<String, Object>, Localization> localizationFunction,
            @NonNull BiFunction<Integer, Integer, List<String>> dataFunction,
            @NonNull Supplier<Long> totalAmountOfElementsSupplier) {
        LOGGER.debug("Initiating a new multipage list... Applying data function...");
        final String data = applyMultipageListDataFunction(user, 0, dataFunction);
        final int amountOfPages = (int)Math.ceil((double)totalAmountOfElementsSupplier.get()
                / NUMBER_OF_ELEMENTS_PER_PAGE_ON_MULTIPAGE_LIST);
        LOGGER.debug("Data function applied and data parsed. Sending the message...");
        final Message message = clientManager.getClient(bot).sendMessage(user,
                localizationFunction.apply(getMultipageListParamMap(amountOfPages, 0, data)));
        LOGGER.debug("Message has been sent. Creating new multipage meta...");

        final MultipageListMeta meta = new MultipageListMeta(ThreadLocalRandom.current().nextInt(
                Integer.MIN_VALUE, Integer.MAX_VALUE), user, bot, message.getMessageId(), 0,
                localizationFunction, dataFunction);
        meta.setAmountOfPages(amountOfPages);
        multipageListMetaRepository.save(meta);
        LOGGER.debug("Multipage meta " + meta.getId() + " has been created and persisted.");
        
        if (meta.getAmountOfPages() != 1) {
            LOGGER.debug("There is more than one page, so a control menu will be attached.");
            initiateMenu(MULTIPAGE_MENU_NAME, user, meta.getId().toString(),
                    message.getMessageId(), bot);
            LOGGER.debug("Menu initiated.");
        }
        return message;
    }

    @Override
    public void processMultipageListRequest(@NonNull MultipageListMeta meta) {
        LOGGER.debug("Updating multipage list message " + meta.getMessageId() + " for user "
                + meta.getUser().getId() + "...");
        final Localization localization = meta.getLocalizationFunction().apply(
                getMultipageListParamMap(meta.getAmountOfPages(), meta.getPage(),
                applyMultipageListDataFunction(meta.getUser(), meta.getPage(),
                meta.getDataFunction())));
        final Menu menu = menuRepository.find(MULTIPAGE_MENU_NAME).get();

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
                    localizationLoader.getLocalizationForUser(ERROR_UPDATE_MESSAGE_FAILURE,
                    meta.getUser()), e);
        }
    }

    @Override
    @NonNull
    public MultipageListMeta getMultipageListMeta(@NonNull Integer id, @NonNull UserEntity user) {
        return multipageListMetaRepository.find(id).orElseThrow(() -> new EntityNotFoundException(
                "Multipage list meta " + id + " does not exist. It might have expired.",
                localizationLoader.getLocalizationForUser(ERROR_MULTIPAGE_LIST_META_NOT_FOUND,
                user)));
    }

    @Override
    @NonNull
    public Menu save(@NonNull Menu menu) {
        return menuRepository.save(menu);
    }

    @Override
    @NonNull
    public MenuTerminationGroup addToMenuTerminationGroup(@NonNull UserEntity user,
            @NonNull UserEntity messagedUser, @NonNull Bot bot, @NonNull Integer messageId,
            @NonNull String key, @Nullable String terminalLocalizationName) {
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

            List<MessageEntity> entities = new ArrayList<>();
            entities.add(messageRepository.save(new MessageEntity(messagedUser, messageId)));
            group.setMessages(entities);
            
            group.setTerminalLocalizationName(terminalLocalizationName);
            group.setUser(user);
        }
        LOGGER.debug("Persisting or updating the MTG...");
        menuTerminationGroupRepository.save(group);
        LOGGER.debug("Operation successful.");
        return group;
    }

    @Override
    public void terminateMenuGroup(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull String key) {
        terminateMenuGroup(user, bot, key, null);
    }

    @Override
    public void terminateMenuGroup(@NonNull UserEntity user, @NonNull Bot bot, @NonNull String key,
            @Nullable Localization terminalLocalizationOverride) {
        final MenuTerminationGroup group = menuTerminationGroupRepository.findByUserIdAndName(
                user.getId(), key).orElseThrow(() -> new EntityNotFoundException(
                "Menu termination group for user " + user.getId() + " and key " + key
                + " does not exist", localizationLoader.getLocalizationForUser(
                ERROR_MTG_NOT_FOUND, user)));
            
        for (MessageEntity message : group.getMessages()) {
            terminateMenu(message.getUser().getId(), message.getMessageId(), bot,
                    (terminalLocalizationOverride != null) ? terminalLocalizationOverride
                    : (group.getTerminalLocalizationName() != null) ? localizationLoader
                    .getLocalizationForUser(group.getTerminalLocalizationName(),
                    message.getUser()) : null);
        }
        menuTerminationGroupRepository.delete(group);
    }

    @Override
    public void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId, @NonNull Bot bot,
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
            
            // throw new TelegramException("Unable to update message "
            //         + messageId + " in chat " + chatId, localizationLoader.getLocalizationForUser(
            //         ERROR_UPDATE_MESSAGE_FAILURE, user), e);
        }
    }

    @Override
    public void terminateMenu(@NonNull Long chatId, @NonNull Integer messageId,
            @NonNull Bot bot) {
        terminateMenu(chatId, messageId, bot, null);
    }

    @Override
    public void answerPotentialCallbackQuery(@NonNull UserEntity user, @NonNull Bot bot)
            throws CallbackQueryAnswerException {
        final Optional<CallbackQuery> query = callbackQueryRepository
                .findAndRemove(user, bot);
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
                    .callbackData(callbackData + b.getData())
                    .text(b.getName())
                    .build())
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

    private String applyMultipageListDataFunction(UserEntity user, int page,
            BiFunction<Integer, Integer, List<String>> dataFunction) {
        final List<String> data = dataFunction.apply(page, NUMBER_OF_ELEMENTS_PER_PAGE_ON_MULTIPAGE_LIST);
        if (data.isEmpty()) {
            throw new NoDataForMultipageListException("No data available for multipage list",
                    localizationLoader.getLocalizationForUser(ERROR_NO_DATA_FOR_MULTIPAGE_LIST,
                    user));
        }
        final StringBuilder builder = new StringBuilder();

        for (String entry : data) {
            builder.append(entry).append('\n').append("-----").append('\n');
        }
        builder.delete(builder.length() - 7, builder.length());
        
        return builder.toString();
    }

    private Map<String, Object> getMultipageListParamMap(int amountOfPages, int page, String data) {
        final Map<String, Object> params = new HashMap<>();
        params.put(PARAM_DATA, data);
        params.put(PARAM_NUMBER_OF_PAGES, amountOfPages);
        params.put(PARAM_CURRENT_PAGE, page + 1);
        return params;
    }
}

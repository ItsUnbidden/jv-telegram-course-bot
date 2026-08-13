package com.unbidden.telegramcoursesbot.menu;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.MenuParamsDto;
import com.unbidden.telegramcoursesbot.dto.internal.MenuSnapshotCreatedDto;
import com.unbidden.telegramcoursesbot.dto.internal.MenuSnapshotUpdatedDto;
import com.unbidden.telegramcoursesbot.dto.internal.TerminalMenuSnapshotUpdatedDto;
import com.unbidden.telegramcoursesbot.dto.internal.TransitoryMenuSnapshotUpdatedDto;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.MenuException;
import com.unbidden.telegramcoursesbot.exception.handler.StaleMenuException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.LinkButton;
import com.unbidden.telegramcoursesbot.menu.handler.AbstractButtonHandler;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.CallbackQueryRepository;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.util.KeyboardUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@RequiredArgsConstructor
public class MenuOrchestrationService {
    private static final Logger LOGGER = LogManager.getLogger(MenuOrchestrationService.class);

    private final Map<String, AbstractButtonHandler> buttonHandlers;

    private final MenuService menuService;

    private final MenuRepository menuRepository;

    private final CallbackQueryRepository callbackQueryRepository;

    private final LocalizationLoader localizationLoader;

    private final KeyboardUtil keyboardUtil;

    private final ClientManager clientManager;

    public Message initiateMenu(UserEntity user, Bot bot, MenuKey key) {
        return initiateMenu0(user, bot, key, 0, Map.of(), null, null, null);
    }

    public Message initiateMenu(UserEntity user, Bot bot, MenuKey key, String paramName, String paramValue) {
        return initiateMenu(user, bot, key, 0, paramName, paramValue);
    }

    public Message initiateMenu(UserEntity user, Bot bot, MenuKey key, String paramName, String paramValue,
            MenuTerminationGroupKey mtgKey, Object... mtgArgs) {
        return initiateMenu0(user, bot, key, 0, Map.of(paramName, paramValue), null, mtgKey, mtgArgs);
    }

    public Message initiateMenu(UserEntity user, Bot bot, MenuKey key, int initialPage, String paramName, String paramValue) {
        return initiateMenu0(user, bot, key, initialPage, Map.of(paramName, paramValue), null, null, null);
    }

    public Message initiateMenu(UserEntity user, Bot bot, MenuKey key, int initialPage, Map<String, String> params) {
        return initiateMenu0(user, bot, key, initialPage, params, null, null, null);
    }

    public void initiateMenu(UserEntity user, Bot bot, MenuKey key, Integer messageId) {
        initiateMenu0(user, bot, key, 0, Map.of(), messageId, null, null);
    }

    public void initiateMenu(UserEntity user, Bot bot, MenuKey key, String paramName, String paramValue, Integer messageId) {
        initiateMenu0(user, bot, key, 0, Map.of(paramName, paramValue), messageId, null, null);
    }

    public void initiateMenu(UserEntity user, Bot bot, MenuKey key, int initialPage, Map<String, String> params, Integer messageId) {
        initiateMenu0(user, bot, key, initialPage, params, messageId, null, null);
    }

    public void initiateMenu(UserEntity user, Bot bot, MenuKey key, String paramName, String paramValue, Integer messageId,
            MenuTerminationGroupKey mtgKey, Object... mtgArgs) {
        initiateMenu0(user, bot, key, 0, Map.of(paramName, paramValue), messageId, mtgKey, mtgArgs);
    }

    // TODO: if a menu fails to be sent, it will break forever on the user's side. Some fallback logic might be required to inform the snapshot.
    public void processCallbackQuery(UserEntity user, Bot bot, CallbackQuery query) {
        final MenuSnapshotUpdatedDto dto = menuService.processSnapshotUpdate(user, bot, Long.parseLong(query.getData()));
        
        if (dto instanceof final TerminalMenuSnapshotUpdatedDto terminalDto) {
            final AbstractButtonHandler handler = buttonHandlers.get(terminalDto.getBeanName());

            if (handler == null) {
                throw new StaleMenuException("An unknown handler name " + terminalDto.getBeanName() + " was found in snapshot "
                        + terminalDto.getSnapshot().getId() + ".", localizationLoader.localize(Localizations.Error.STALE_MENU, user));
            }

            RuntimeException potentialExc = null;
            try {
                LOGGER.trace("Handler " + terminalDto.getBeanName() + " has been found. Executing...");
                handler.handle(user, bot, terminalDto.getParams());
                LOGGER.trace("Handler " + terminalDto.getBeanName() + " has finished execution.");
            } catch (RuntimeException e) {
                LOGGER.trace("An exception has occured while executing the handler's function. It will be rethrown after the menu is updated.");
                potentialExc = e;
            }

            if (terminalDto.getNextPage() != null && terminalDto.getButtons() != null && terminalDto.getSnapshotButtons() != null) {
                LOGGER.trace("The initial layout will be sent...");
                try {
                    if (terminalDto.getNextPage().getLocalizationFunction() == null) {
                        LOGGER.trace("Sending new message markup...");
                        clientManager.getClient(bot).execute(EditMessageReplyMarkup.builder()
                                .chatId(user.getId())
                                .messageId(terminalDto.getSnapshot().getMessageId())
                                .replyMarkup(getMarkup(user, bot, terminalDto.getNextPage(),
                                    terminalDto.getSnapshotButtons(), terminalDto.getButtons()))
                                .build());
                        LOGGER.trace("New markup sent.");
                    } else {
                        LOGGER.trace("Sending new message content and markup...");
                        final Localization loc = terminalDto.getNextPage().getLocalizationFunction()
                                .apply(new MenuParamsDto(user, bot, terminalDto.getParams(), terminalDto.getSnapshot().getInitialPage()));

                        clientManager.getClient(bot).execute(EditMessageText.builder()
                                .chatId(user.getId())
                                .messageId(terminalDto.getSnapshot().getMessageId())
                                .replyMarkup(getMarkup(user, bot, terminalDto.getNextPage(),
                                    terminalDto.getSnapshotButtons(), terminalDto.getButtons()))
                                .text(loc.getData())
                                .entities(loc.getEntities())
                                .build());
                        LOGGER.trace("New content and markup sent.");
                    }  
                } catch (TelegramApiException e) {
                    LOGGER.error("Unable to update message " + query.getMessage()
                            .getMessageId() + " and user " + user.getId(), e);
                }

                if (potentialExc != null) {
                    throw potentialExc;
                }
                return;
            }
            if (terminalDto.isTerminate()) {
                if (terminalDto.getNextPage() != null) {
                    LOGGER.trace("The menu is supposed to be terminated with a custom terminal localization.");
                    if (terminalDto.getNextPage().getLocalizationFunction() != null) {
                        final Localization loc = terminalDto.getNextPage().getLocalizationFunction().apply(new MenuParamsDto(user, bot,
                                terminalDto.getParams(), terminalDto.getSnapshot().getInitialPage()));

                        try {
                            LOGGER.trace("Sending new message content and clear markup...");
                            clientManager.getClient(bot).execute(EditMessageText.builder()
                                    .chatId(user.getId())
                                    .messageId(terminalDto.getSnapshot().getMessageId())
                                    .replyMarkup(InlineKeyboardMarkup.builder()
                                        .keyboard(List.of())
                                        .clearKeyboard()
                                        .build())
                                    .text(loc.getData())
                                    .entities(loc.getEntities())
                                    .build());
                            LOGGER.trace("New content and clear markup sent.");
                        } catch (TelegramApiException e) {
                            LOGGER.error("Unable to update content and markup for message " + query.getMessage()
                                    .getMessageId() + " and user " + user.getId(), e);
                        }

                        if (potentialExc != null) {
                            throw potentialExc;
                        }
                        return;
                    } else {
                        LOGGER.warn("Menu " + terminalDto.getNextPage().getMenu().getKey() + " is supposed to be terminated with a custom "
                                + "localization function, but the function is currently null. If this is intentional, the terminal page should be removed entirely.");
                    }
                }
                LOGGER.trace("The menu is supposed to be terminated with no changes to the message contents.");
                try {
                    LOGGER.trace("Sending clear markup...");
                    clientManager.getClient(bot).execute(EditMessageReplyMarkup.builder()
                            .chatId(user.getId())
                            .messageId(terminalDto.getSnapshot().getMessageId())
                            .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(List.of())
                                .clearKeyboard()
                                .build())
                            .build());
                    LOGGER.trace("Clear markup sent.");
                } catch (TelegramApiException e) {
                    LOGGER.error("Unable to update markup for message " + query.getMessage()
                            .getMessageId() + " and user " + user.getId(), e);
                }
                
                if (potentialExc != null) {
                    throw potentialExc;
                }
                return;
            }
        } else if (dto instanceof final TransitoryMenuSnapshotUpdatedDto transitoryDto) {
            LOGGER.trace("Transitioning to the new page...");

            try {
                if (transitoryDto.getNextPage().getLocalizationFunction() == null) {
                    LOGGER.trace("Sending new message markup...");
                    clientManager.getClient(bot).execute(EditMessageReplyMarkup.builder()
                            .chatId(user.getId())
                            .messageId(transitoryDto.getSnapshot().getMessageId())
                            .replyMarkup(getMarkup(user, bot, transitoryDto.getNextPage(),
                                transitoryDto.getSnapshotButtons(), transitoryDto.getButtons()))
                            .build());
                    LOGGER.trace("New markup sent.");
                    return;
                } else {
                    LOGGER.trace("Sending new message content and markup...");
                    final Localization loc = transitoryDto.getNextPage().getLocalizationFunction()
                            .apply(new MenuParamsDto(user, bot, transitoryDto.getParams(), transitoryDto.getSnapshot().getInitialPage()));

                    clientManager.getClient(bot).execute(EditMessageText.builder()
                            .chatId(user.getId())
                            .messageId(transitoryDto.getSnapshot().getMessageId())
                            .replyMarkup(getMarkup(user, bot, transitoryDto.getNextPage(),
                                transitoryDto.getSnapshotButtons(), transitoryDto.getButtons()))
                            .text(loc.getData())
                            .entities(loc.getEntities())
                            .build());
                    LOGGER.trace("New content and markup sent.");
                    return;
                }  
            } catch (TelegramApiException e) {
                LOGGER.error("Unable to update message " + query.getMessage()
                        .getMessageId() + " and user " + user.getId(), e);
                return;
            }
        } else {
            throw new MenuException("An unknown response DTO was returned by the transactional service. This is a bug.", null);
        }
    }

    public Message initiateMultipageList(UserEntity user, Bot bot,
            Function<MultipageListParams, Localization> localizationFunction,
            BiFunction<Integer, Integer, org.springframework.data.domain.Page<String>> dataFunction) {
        throw new ForbiddenOperationException("Multipage lists are currently unimplemented.", null);

        // LOGGER.debug("Initiating a new multipage list... Applying data function...");
        // final org.springframework.data.domain.Page<String> dataPage = dataFunction.apply(0,
        //         NUMBER_OF_ELEMENTS_PER_PAGE_ON_MULTIPAGE_LIST);

        // if (dataPage.getTotalElements() == 0) {
        //     throw new NoDataForMultipageListException("No data available for multipage list",
        //             localizationLoader.localize(Error.NO_DATA_FOR_MULTIPAGE_LIST,
        //             user));
        // }
        // final String data = convertDataPageToString(dataPage);
        
        // LOGGER.debug("Data function applied and data parsed. Sending the message...");
        // final Message message = clientManager.getClient(bot).sendMessage(user,
        //         localizationFunction.apply(new MultipageListParams(0, dataPage.getTotalPages(),
        //         dataPage.getTotalElements(), data)));

        // LOGGER.debug("Message has been sent.");

        // if (dataPage.getTotalPages() > 1) {
        //     LOGGER.debug("There is more than one page. Creating new multipage meta...");
        //     final MultipageListMeta meta = new MultipageListMeta(UUID.randomUUID(), user, bot,
        //             message.getMessageId(), 0, localizationFunction, dataFunction);

        //     meta.setNumberOfElements(dataPage.getTotalElements());
        //     meta.setNumberOfPages(dataPage.getTotalPages());
        //     multipageListMetaRepository.save(meta);
        //     LOGGER.debug("Multipage meta " + meta.getId() + " has been created and persisted.");

        //     LOGGER.debug("Attaching a control menu...");
        //     initiateMenu(user, bot, MenuKey.MULTIPAGE_LIST, MUTLIPAGE_LIST_PARAM, meta.getId().toString(), message.getMessageId());
        //     LOGGER.debug("Menu initiated.");
        // }
        // return message;
    }

    // public void processMultipageListRequest(MultipageListMeta meta) {
    //     LOGGER.debug("Updating multipage list message " + meta.getMessageId() + " for user "
    //             + meta.getUser().getId() + "...");
    //     final Localization localization = meta.getLocalizationFunction().apply(
    //             new MultipageListParams(meta.getPage(), meta.getNumberOfPages(), meta.getNumberOfElements(),
    //             convertDataPageToString(meta.getDataFunction().apply(meta.getPage(), NUMBER_OF_ELEMENTS_PER_PAGE_ON_MULTIPAGE_LIST))));
    //     final Menu menu = menuRepository.find(MenuKey.MULTIPAGE_LIST).get();

    //     try {
    //         clientManager.getClient(meta.getBot()).execute(EditMessageText.builder()
    //                 .chatId(meta.getUser().getId())
    //                 .messageId(meta.getMessageId())
    //                 .text(localization.getData())
    //                 .entities(localization.getEntities())
    //                 .replyMarkup(getInitialMarkup(menu.getPages().getFirst(),
    //                     meta.getId().toString(), meta.getUser(), meta.getBot()))
    //                 .build());
    //     } catch (TelegramApiException e) {
    //         throw new TelegramException("Unable to update a multipage list message "
    //                 + meta.getMessageId() + " for user " + meta.getUser().getId(),
    //                 localizationLoader.localize(Error.UPDATE_MESSAGE_FAILURE,
    //                 meta.getUser()), e);
    //     }
    // }

    // public MultipageListMeta getMultipageListMeta(UUID id, UserEntity user) {
    //     return multipageListMetaRepository.find(id).orElseThrow(() -> new EntityNotFoundException(
    //             "Multipage list meta " + id + " does not exist. It might have expired.",
    //             localizationLoader.localize(Error.MULTIPAGE_LIST_META_NOT_FOUND,
    //             user)));
    // }

    public Menu save(Menu menu) {
        return menuRepository.save(menu);
    }

    /**
     * Removes the menus for all messages in the specified menu termination group. If a menu cannot be removed, the exception will be ignored.
     * @param user with whom the group is associated.
     * @param bot
     * @param key that identifies this group.
     * @param args that will be used to format the MTG key.
     */
    public void terminateMenuGroup(MenuTerminationGroupKey key, Object... args) {
        terminateMenuGroup(key, null, args);
    }

    /**
     * Removes the menus for all messages in the specified menu group. If a menu cannot be removed, the exception will be ignored.
     * @param key that identifies this group.
     * @param terminalLocalizationOverride — the key of the localization that will be used to replace the message's text after the menu is removed. If a menu contained a terminal page, it will be overridden.
     * @param args that will be used to format the group key.
     */
    public void terminateMenuGroup(MenuTerminationGroupKey key, @Nullable Localization terminalLocalizationOverride, Object... args) {
        final List<MenuSnapshot> snapshots = menuService.terminateMenus(key, args);
        
        for (final MenuSnapshot snapshot : snapshots) {
            final Optional<Menu> menuOpt = menuRepository.find(snapshot.getKey());

            terminateMenu(snapshot.getUser().getId(), snapshot.getMessageId(), snapshot.getBot(),
                    (terminalLocalizationOverride != null) ? terminalLocalizationOverride
                    : (menuOpt.isPresent() && menuOpt.get().getTerminalPage() != null) 
                        ? menuOpt.get().getTerminalPage().getLocalizationFunction().apply(
                            new MenuParamsDto(snapshot.getUser(), snapshot.getBot(), snapshot.paramsToMap(), snapshot.getInitialPage()))
                        : null);
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

    private Message initiateMenu0(UserEntity user, Bot bot, MenuKey key, int initialPage, Map<String, String> params,
            Integer messageId, MenuTerminationGroupKey mtgKey, Object[] mtgArgs) {
        final Menu menu = menuRepository.find(key).orElseThrow(() ->
                new EntityNotFoundException("Menu " + key + " was not found",
                localizationLoader.localize(Error.MENU_NOT_FOUND, user)));
        final Page firstPage = menu.getPages().get(initialPage);
        final MenuParamsDto dto = new MenuParamsDto(user, bot, params, initialPage);
        final List<Button> generatedLayout = firstPage.getButtonsFunction().apply(dto);
        final MenuSnapshotCreatedDto snapshotDto = menuService.createSnapshot(user, bot, key, initialPage,
                    generatedLayout, params, messageId, mtgKey, mtgArgs);
        final InlineKeyboardMarkup markup = getMarkup(user, bot, firstPage, snapshotDto.buttons(), generatedLayout);

        if (messageId == null) {
            final Localization localization = firstPage.getLocalizationFunction().apply(dto);

            LOGGER.trace("Sending menu " + menu.getKey() + " to user " + user.getId() + "...");
            final Message message = clientManager.getClient(bot).sendMessage(user, localization, markup);
            LOGGER.trace("Message sent. Adding the new message's ID to snapshot " + snapshotDto.snapshot().getId() + "...");
            menuService.addMessageIdToSnapshot(snapshotDto.snapshot().getId(), messageId);
            LOGGER.trace("Message ID added to snapshot " + snapshotDto.snapshot().getId() + ".");

            return message;
        }
        LOGGER.trace("Attaching menu" + key + "'s markup to message " + messageId + " for user " + user.getId() + "...");
        final var editMessageReplyMarkup = EditMessageReplyMarkup.builder()
                .chatId(user.getId())
                .messageId(messageId)
                .replyMarkup(markup)
                .build();
        try {
            clientManager.getClient(bot).execute(editMessageReplyMarkup);
            LOGGER.trace("Markup sent.");
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to update markup for message " + messageId + " for user "
                    + user.getId(), e);
        }
        return null;
    }

    private InlineKeyboardMarkup getMarkup(UserEntity user, Bot bot, Page page,
            List<MenuSnapshotButton> snapshotButtons, List<Button> buttons) {
        final List<InlineKeyboardButton> inlineButtons = new ArrayList<>();

        for (int i = 0; i < buttons.size(); ++i) {
            final Button button = buttons.get(i);
            final MenuSnapshotButton snapshotButton = snapshotButtons.get(i);

            inlineButtons.add((InlineKeyboardButton)InlineKeyboardButton.builder()
                    .callbackData(!button.getClass().equals(LinkButton.class) ? snapshotButton.getId().toString() : null)
                    .url(button.getClass().equals(LinkButton.class) ? ((LinkButton)button).getUrl() : null)
                    .text(button.getName())
                    .build());
        }
                
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardUtil.getInlineKeyboard(inlineButtons, page.getColumns()))
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

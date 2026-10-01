package com.unbidden.telegramcoursesbot.menu;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.dto.internal.MenuParamsDto;
import com.unbidden.telegramcoursesbot.dto.internal.MenuSnapshotCreatedDto;
import com.unbidden.telegramcoursesbot.dto.internal.MenuSnapshotWithButtonsDto;
import com.unbidden.telegramcoursesbot.dto.internal.MultipageListData;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto.Result;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.multipage.MultipageListDataConverterManager;
import com.unbidden.telegramcoursesbot.menu.multipage.supplier.AbstractDataSupplier;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.GeneralMenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MultipageListMenuSnapshot;
import com.unbidden.telegramcoursesbot.repository.CallbackQueryRepository;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.MenuUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendRichMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.richtext.InputRichMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@RequiredArgsConstructor
public class MenuOrchestrationService {
    private static final Logger LOGGER = LogManager.getLogger(MenuOrchestrationService.class);

    private final MenuService menuService;

    private final MultipageListDataConverterManager converterManager;

    private final MenuRepository menuRepository;

    private final CallbackQueryRepository callbackQueryRepository;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    private final MenuUtil keyboardUtil;

    private final ValidatorUtil validatorUtil;

    private final MenuUtil menuUtil;

    public Message initiateMenu(BotRole botRole, MenuKey key) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");

        return initiateMenu0(botRole, key, 0, Map.of(), null, null, null);
    }

    public Message initiateMenu(BotRole botRole, MenuKey key, String paramName, String paramValue) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(paramName, "paramName cannot be null");
        Assert.notNull(paramValue, "paramValue cannot be null");

        return initiateMenu(botRole, key, 0, paramName, paramValue);
    }

    public Message initiateMenu(BotRole botRole, MenuKey key, String paramName, String paramValue,
            MenuTerminationGroupKey mtgKey, Object... mtgArgs) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(paramName, "paramName cannot be null");
        Assert.notNull(paramValue, "paramValue cannot be null");
        Assert.notNull(mtgKey, "mtgKey cannot be null");

        return initiateMenu0(botRole, key, 0, Map.of(paramName, paramValue), null, mtgKey, mtgArgs);
    }

    public Message initiateMenu(BotRole botRole, MenuKey key, int initialPage, String paramName, String paramValue) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(paramName, "paramName cannot be null");
        Assert.notNull(paramValue, "paramValue cannot be null");
        
        return initiateMenu0(botRole, key, initialPage, Map.of(paramName, paramValue), null, null, null);
    }

    public Message initiateMenu(BotRole botRole, MenuKey key, int initialPage, Map<String, String> params) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(params, "params cannot be null");

        return initiateMenu0(botRole, key, initialPage, params, null, null, null);
    }

    public void initiateMenu(BotRole botRole, MenuKey key, Integer messageId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        
        initiateMenu0(botRole, key, 0, Map.of(), messageId, null, null);
    }

    public void initiateMenu(BotRole botRole, MenuKey key, String paramName, String paramValue, Integer messageId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(paramName, "paramName cannot be null");
        Assert.notNull(paramValue, "paramValue cannot be null");
        Assert.notNull(messageId, "messageId cannot be null");
        
        initiateMenu0(botRole, key, 0, Map.of(paramName, paramValue), messageId, null, null);
    }

    public void initiateMenu(BotRole botRole, MenuKey key, int initialPage, Map<String, String> params, Integer messageId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(params, "params cannot be null");
        Assert.notNull(messageId, "messageId cannot be null");

        initiateMenu0(botRole, key, initialPage, params, messageId, null, null);
    }

    public void initiateMenu(BotRole botRole, MenuKey key, int initialPage, Map<String, String> params, Integer messageId,
            MenuTerminationGroupKey mtgKey, Object... mtgArgs) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(params, "params cannot be null");
        Assert.notNull(messageId, "messageId cannot be null");
        Assert.notNull(mtgKey, "mtgKey cannot be null");

        initiateMenu0(botRole, key, initialPage, params, messageId, mtgKey, mtgArgs);
    }

    public void initiateMenu(BotRole botRole, MenuKey key, String paramName, String paramValue, Integer messageId,
            MenuTerminationGroupKey mtgKey, Object... mtgArgs) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(paramName, "paramName cannot be null");
        Assert.notNull(paramValue, "paramValue cannot be null");
        Assert.notNull(messageId, "messageId cannot be null");
        Assert.notNull(mtgKey, "mtgKey cannot be null");

        initiateMenu0(botRole, key, 0, Map.of(paramName, paramValue), messageId, mtgKey, mtgArgs);
    }

    public void initiateMultipageList(BotRole botRole, AbstractDataSupplier supplier) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(supplier, "supplier cannot be null");

        initiateMultipageList0(botRole, supplier, Map.of());
    }

    public void initiateMultipageList(BotRole botRole, AbstractDataSupplier supplier, String paramName, String paramValue) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(supplier, "supplier cannot be null");
        Assert.notNull(paramName, "paramName cannot be null");
        Assert.notNull(paramValue, "paramValue cannot be null");

        initiateMultipageList0(botRole, supplier, Map.of(paramName, paramValue));
    }

    public void initiateMultipageList(BotRole botRole, AbstractDataSupplier supplier, Map<String, String> params) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(supplier, "supplier cannot be null");
        Assert.notNull(params, "params cannot be null");

        initiateMultipageList0(botRole, supplier, params);
    }

    public Menu save(Menu menu) {
        Assert.notNull(menu, "menu cannot be null");

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
        Assert.notNull(key, "key cannot be null");

        terminateMenuGroup(key, null, args);
    }

    /**
     * Removes the menus for all messages in the specified menu group. If a menu cannot be removed, the exception will be ignored.
     * @param key that identifies this group.
     * @param terminalLocalizationOverride — the key of the localization that will be used to replace the message's text after the menu is removed. If a menu contained a terminal page, it will be overridden.
     * @param args that will be used to format the group key.
     */
    public void terminateMenuGroup(MenuTerminationGroupKey key, @Nullable Localization terminalLocalizationOverride, Object... args) {
        Assert.notNull(key, "key cannot be null");
        
        final List<MenuSnapshot> snapshots = menuService.terminateMenus(key, args);
        
        for (final MenuSnapshot snapshot : snapshots) {
            final Localization terminalLoc;

            if (snapshot instanceof final GeneralMenuSnapshot generalSnapshot) {
                final Optional<Menu> menuOpt = menuRepository.find(generalSnapshot.getKey());

                if (terminalLocalizationOverride != null) {
                    terminalLoc = terminalLocalizationOverride;
                } else {
                    if (menuOpt.isPresent() && menuOpt.get().getTerminalPage() != null) {
                        terminalLoc = menuOpt.get().getTerminalPage().getLocalizationFunction().apply(
                            new MenuParamsDto(generalSnapshot.getBotRole(), menuUtil.stringToMap(generalSnapshot.getParameters()),
                            menuUtil.stringToIntList(generalSnapshot.getPageHistory())));
                    } else {
                        terminalLoc = null;
                    }
                }
            } else {
                terminalLoc = null;
            }

            terminateMenu(snapshot.getBotRole().getUser().getId(), snapshot.getMessageId(), snapshot.getBotRole().getBot(), terminalLoc);
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
        Assert.notNull(chatId, "chatId cannot be null");
        Assert.notNull(messageId, "messageId cannot be null");
        Assert.notNull(bot, "bot cannot be null");

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

    public void terminateMenuManually(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);
        final Long snapshotId = validatorUtil.parseId(botRole, messages.getFirst());
        final MenuSnapshot snapshot = menuService.terminateMenu(botRole, snapshotId);
        final InlineKeyboardMarkup clearMarkup = InlineKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of())
                .build();

        try {
            final Localization terminalLoc = localizationLoader.localize(Localizations.Service.MENU_MANUALLY_REMOVED, botRole);

            clientManager.getClient(snapshot.getBotRole().getBot()).execute(EditMessageText.builder()
                    .chatId(snapshot.getBotRole().getUser().getId())
                    .messageId(snapshot.getMessageId())
                    .text(terminalLoc.getData())
                    .entities(terminalLoc.getEntities())
                    .replyMarkup(clearMarkup)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Failed to update message " + snapshot.getMessageId() + " for user "
                    + snapshot.getBotRole().getId() + " in bot " + snapshot.getBotRole().getBot().getId() + ".",
                    localizationLoader.localize(Localizations.Error.MENU_MANUALLY_REMOVED_FAILED, botRole), e);
        }
        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.MENU_MANUALLY_REMOVED_SUCCESS, botRole));
    }

    public void terminateMenu(BotRole botRole, Long snapshotId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(snapshotId, "snapshotId cannot be null");

        terminateMenu(botRole, snapshotId, null);
    }

    public void terminateMenu(BotRole botRole, Long snapshotId, Localization terminalLocOverride) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(snapshotId, "snapshotId cannot be null");
        Assert.notNull(terminalLocOverride, "terminalLocOverride cannot be null");

        final MenuSnapshot snapshot = menuService.terminateMenu(botRole, snapshotId);
        final InlineKeyboardMarkup clearMarkup = InlineKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of())
                .build();

        try {
            if (snapshot instanceof final GeneralMenuSnapshot generalSnapshot) {
                final Optional<Menu> menuOpt = menuRepository.find(generalSnapshot.getKey());

                if (menuOpt.isEmpty()) {
                    LOGGER.error("Menu " + generalSnapshot.getKey() + " does not exist. This is a bug.");
                    return;
                }
                final Menu menu = menuOpt.get();
                final Localization terminalLoc;

                if (terminalLocOverride != null) {
                    terminalLoc = terminalLocOverride;
                } else if (menu.getTerminalPage() != null) {
                    terminalLoc = menu.getTerminalPage().getLocalizationFunction().apply(new MenuParamsDto(snapshot.getBotRole(),
                            menuUtil.stringToMap(generalSnapshot.getParameters()),
                            menuUtil.stringToIntList(generalSnapshot.getPageHistory())));
                } else {
                    terminalLoc = null;
                }
    
                if (terminalLoc == null) {
                    clientManager.getClient(snapshot.getBotRole().getBot()).execute(DeleteMessage.builder()
                            .chatId(snapshot.getBotRole().getUser().getId())
                            .messageId(snapshot.getMessageId())
                            .build());
                } else {
                    clientManager.getClient(snapshot.getBotRole().getBot()).execute(EditMessageText.builder()
                            .chatId(snapshot.getBotRole().getUser().getId())
                            .messageId(snapshot.getMessageId())
                            .text(terminalLoc.getData())
                            .entities(terminalLoc.getEntities())
                            .replyMarkup(clearMarkup)
                            .build());
                }
            }
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to update or delete message " + snapshot.getMessageId() + " for user "
                    + snapshot.getBotRole().getUser().getId() + " in bot " + snapshot.getBotRole().getBot().getId() + ".");
        }
    }

    public void refreshMenuLocalization(BotRole botRole, Long snapshotId) {
        final MenuSnapshotWithButtonsDto dto = menuService.getMenuSnapshotWithButtons(botRole, snapshotId);

        if (dto.snapshot() instanceof GeneralMenuSnapshot generalSnapshot) {
            final Optional<Menu> menuOpt = menuRepository.find(generalSnapshot.getKey());

                if (menuOpt.isEmpty()) {
                    LOGGER.error("Menu " + generalSnapshot.getKey() + " does not exist. This is a bug.");
                    return;
                }
                final Page page = menuOpt.get().getPages().get(generalSnapshot.getCurrentPage());
                final MenuParamsDto params = new MenuParamsDto(generalSnapshot.getBotRole(),
                        menuUtil.stringToMap(generalSnapshot.getParameters()),
                        menuUtil.stringToIntList(generalSnapshot.getPageHistory()));
                final Localization loc = page.getLocalizationFunction().apply(params);

            try {
                clientManager.getClient(generalSnapshot.getBotRole().getBot()).execute(EditMessageText.builder()
                        .chatId(generalSnapshot.getBotRole().getUser().getId())
                        .messageId(generalSnapshot.getMessageId())
                        .text(loc.getData())
                        .entities(loc.getEntities())
                        .replyMarkup(menuUtil.getMarkup(page, dto.buttons(), page.getButtonsFunction().apply(params)))
                        .build());
            } catch (TelegramApiException e) {
                LOGGER.error("Unable to update message " + generalSnapshot.getMessageId() + " for user "
                        + generalSnapshot.getBotRole().getUser().getId() + " in bot "
                        + generalSnapshot.getBotRole().getBot().getId() + ".");
            }
        } else {
            LOGGER.error("Unable to refresh the localization of menu snapshot "
                    + snapshotId + " because it's not a general snapshot. This is a bug.");
        }
    }

    public void terminateMenusForUserInBot(BotRole callerBotRole, List<Message> messages) {
        validatorUtil.checkExactExpectedMessages(callerBotRole, messages, 2);
        final BotRole targetBotRole = entityUtil.getActiveBotRole(callerBotRole,
                validatorUtil.parseId(callerBotRole, messages.getLast()),
                validatorUtil.parseId(callerBotRole, messages.getFirst()));
        final List<MenuSnapshot> snapshots = menuService.terminateMenus(targetBotRole.getId());
        final Localization terminalLoc = localizationLoader.localize(Localizations.Service.MENU_MANUALLY_REMOVED, targetBotRole);
        final CustomTelegramClient client = clientManager.getClient(targetBotRole.getBot());
        final InlineKeyboardMarkup clearMarkup = InlineKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of())
                .build();
        final List<CompletableFuture<?>> futures = new ArrayList<>();

        LOGGER.info("Sending message update requests for user " + targetBotRole.getUser().getId()
                + " in bot " + targetBotRole.getBot().getId() + ".");
        for (final MenuSnapshot snapshot : snapshots) {
            try {
                futures.add(client.executeAsync(EditMessageText.builder()
                        .chatId(targetBotRole.getUser().getId())
                        .messageId(snapshot.getMessageId())
                        .text(terminalLoc.getData())
                        .entities(terminalLoc.getEntities())
                        .replyMarkup(clearMarkup)
                        .build()).handle((r, t) -> t == null ? r : null));
            } catch (TelegramApiException e) {
                futures.add(CompletableFuture.completedFuture(null));
            }
        }
        LOGGER.info("Requests have been sent. Waiting for completion...");
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        LOGGER.info("All requests have been completed.");
        final int successes = (int)futures.stream().filter(f -> f.join() != null).count();
        final int failures = futures.size() - successes;
        
        clientManager.sendMessage(callerBotRole, localizationLoader.localize(
                Localizations.Service.MENUS_MANUALLY_REMOVED_SUCCESS, callerBotRole,
                new Localizations.Service.MenusManuallyRemovedSuccessParams(
                    successes, failures)));
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

    public void answerPotentialCallbackQuery(BotRole botRole) throws CallbackQueryAnswerException {
        final Optional<CallbackQuery> query = callbackQueryRepository.findAndRemove(botRole);

        if (query.isPresent()) {
            LOGGER.debug("User " + botRole.getUser().getId() + " has an unanswered callback query.");
            try {
                clientManager.getClient(botRole.getBot()).execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(query.get().getId())
                        .build());
                LOGGER.debug("Callback query resolved.");
            } catch (TelegramApiException e) {
                throw new CallbackQueryAnswerException("Unable to answer callback query", e);
            }
        }
    }

    private Message initiateMenu0(BotRole botRole, MenuKey key, int initialPage, Map<String, String> params,
            Integer messageId, MenuTerminationGroupKey mtgKey, Object[] mtgArgs) {
        final Menu menu = menuRepository.find(key).orElseThrow(() ->
                new EntityNotFoundException("Menu " + key + " was not found",
                localizationLoader.localize(Error.MENU_NOT_FOUND, botRole)));
        final Page firstPage = menu.getPages().get(initialPage);
        final MenuParamsDto dto = new MenuParamsDto(botRole, params, List.of());
        final List<Button> generatedLayout = firstPage.getButtonsFunction().apply(dto);
        final MenuSnapshotCreatedDto snapshotDto = menuService.createSnapshot(botRole, key, initialPage,
                generatedLayout, params, messageId, mtgKey, mtgArgs);
        final InlineKeyboardMarkup markup = keyboardUtil.getMarkup(firstPage, snapshotDto.buttons(), generatedLayout);

        if (messageId == null) {
            final Localization localization = firstPage.getLocalizationFunction().apply(dto);

            LOGGER.trace("Sending menu " + menu.getKey() + " to user " + botRole.getUser().getId() + "...");
            final SendMessageResultDto result = clientManager.sendMessage(botRole, localization, markup);

            if (result.getResult() == Result.OK) {
                LOGGER.trace("Message sent. Adding the new message's ID to snapshot " + snapshotDto.snapshot().getId() + "...");
                menuService.addMessageIdToSnapshot(snapshotDto.snapshot().getId(), result.getMessage().getMessageId());
                LOGGER.trace("Message ID added to snapshot " + snapshotDto.snapshot().getId() + ".");
    
                return result.getMessage();
            } else {
                return null; // TODO: introduce fallback
            }
        }
        LOGGER.trace("Attaching menu " + key + "'s markup to message " + messageId + " for user " + botRole.getUser().getId() + "...");
        final var editMessageReplyMarkup = EditMessageReplyMarkup.builder()
                .chatId(botRole.getUser().getId())
                .messageId(messageId)
                .replyMarkup(markup)
                .build();

        try {
            clientManager.getClient(botRole.getBot()).execute(editMessageReplyMarkup);
            LOGGER.trace("Markup sent.");
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to update markup for message " + messageId + " for user " + botRole.getUser().getId(), e);
            // TODO: introduce fallback
        }
        return null;
    }

    private void initiateMultipageList0(BotRole botRole, AbstractDataSupplier supplier, Map<String, String> params) {
        final MultipageListMenuSnapshot snapshot = menuService.createMultipageListSnapshot(botRole, supplier, params);

        LOGGER.debug("Fetching data for multipage list from " + supplier.getBeanName() + "...");
        final MultipageListData data = supplier.fetchData(botRole, 0, params);

        LOGGER.debug("Data aquired. Compiling and sending the message...");
        final InputRichMessage richMessage = converterManager.getConverter(data.getClass()).convert(botRole, data);

        try {
            final Message message = clientManager.getClient(botRole.getBot())
                .execute(SendRichMessage.builder()
                    .chatId(botRole.getUser().getId())
                    .richMessage(richMessage)
                    .replyMarkup(menuUtil.getMultipageListMarkup(botRole, snapshot, data))
                    .build()
                );

            menuService.addMessageIdToSnapshot(snapshot.getId(), message.getMessageId());
            LOGGER.debug("Multipage list sent.");
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to send rich message for multipage list to user " + botRole.getUser().getId()
                    + " in bot " + botRole.getBot().getId() + ".", e);
            // TODO: introduce fallback
        }
    }
}

package com.unbidden.telegramcoursesbot.menu;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.MenuParamsDto;
import com.unbidden.telegramcoursesbot.dto.internal.MenuSnapshotUpdatedDto;
import com.unbidden.telegramcoursesbot.dto.internal.TerminalMenuSnapshotUpdatedDto;
import com.unbidden.telegramcoursesbot.dto.internal.TransitoryMenuSnapshotUpdatedDto;
import com.unbidden.telegramcoursesbot.exception.MenuException;
import com.unbidden.telegramcoursesbot.exception.handler.StaleMenuException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.handler.AbstractButtonHandler;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.repository.CallbackQueryRepository;
import com.unbidden.telegramcoursesbot.util.KeyboardUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuCallbackRequestProcessor {
    private static final Logger LOGGER = LogManager.getLogger(MenuCallbackRequestProcessor.class);

    private final Map<String, AbstractButtonHandler> buttonHandlers;

    private final MenuService menuService;

    private final CallbackQueryRepository callbackQueryRepository;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final KeyboardUtil keyboardUtil;
    
    // TODO: if a menu fails to be sent, it will break forever on the user's side. Some fallback logic might be required to inform the snapshot.
    public void processCallbackQuery(BotRole botRole, CallbackQuery query) {
        callbackQueryRepository.save(botRole, query);

        final MenuSnapshotUpdatedDto dto = menuService.processSnapshotUpdate(botRole, Long.parseLong(query.getData()));
        
        if (dto instanceof final TerminalMenuSnapshotUpdatedDto terminalDto) {
            final AbstractButtonHandler handler = buttonHandlers.get(terminalDto.getBeanName());

            if (handler == null) {
                throw new StaleMenuException("An unknown handler name " + terminalDto.getBeanName() + " was found in snapshot "
                        + terminalDto.getSnapshot().getId() + ".", loader.localize(Localizations.Error.STALE_MENU, botRole));
            }

            RuntimeException potentialExc = null;
            try {
                LOGGER.trace("Handler " + terminalDto.getBeanName() + " has been found. Executing...");
                handler.handle(botRole, terminalDto.getParams());
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
                        clientManager.getClient(botRole.getBot()).execute(EditMessageReplyMarkup.builder()
                                .chatId(botRole.getUser().getId())
                                .messageId(terminalDto.getSnapshot().getMessageId())
                                .replyMarkup(keyboardUtil.getMarkup(terminalDto.getNextPage(),
                                    terminalDto.getSnapshotButtons(), terminalDto.getButtons()))
                                .build());
                        LOGGER.trace("New markup sent.");
                    } else {
                        LOGGER.trace("Sending new message content and markup...");
                        final Localization loc = terminalDto.getNextPage().getLocalizationFunction()
                                .apply(new MenuParamsDto(botRole, terminalDto.getParams(), terminalDto.getSnapshot().getInitialPage()));

                        clientManager.getClient(botRole.getBot()).execute(EditMessageText.builder()
                                .chatId(botRole.getUser().getId())
                                .messageId(terminalDto.getSnapshot().getMessageId())
                                .replyMarkup(keyboardUtil.getMarkup(terminalDto.getNextPage(),
                                    terminalDto.getSnapshotButtons(), terminalDto.getButtons()))
                                .text(loc.getData())
                                .entities(loc.getEntities())
                                .build());
                        LOGGER.trace("New content and markup sent.");
                    }  
                } catch (TelegramApiException e) {
                    LOGGER.error("Unable to update message " + query.getMessage().getMessageId() + " and user " + botRole.getUser().getId(), e);

                    // TODO: introduce fallback
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
                        final Localization loc = terminalDto.getNextPage().getLocalizationFunction().apply(new MenuParamsDto(botRole,
                                terminalDto.getParams(), terminalDto.getSnapshot().getInitialPage()));

                        try {
                            LOGGER.trace("Sending new message content and clear markup...");
                            clientManager.getClient(botRole.getBot()).execute(EditMessageText.builder()
                                    .chatId(botRole.getUser().getId())
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
                                    .getMessageId() + " and user " + botRole.getUser().getId(), e);

                            // TODO: introduce fallback
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
                    clientManager.getClient(botRole.getBot()).execute(EditMessageReplyMarkup.builder()
                            .chatId(botRole.getUser().getId())
                            .messageId(terminalDto.getSnapshot().getMessageId())
                            .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(List.of())
                                .clearKeyboard()
                                .build())
                            .build());
                    LOGGER.trace("Clear markup sent.");
                } catch (TelegramApiException e) {
                    LOGGER.error("Unable to update markup for message " + query.getMessage()
                            .getMessageId() + " and user " + botRole.getUser().getId(), e);

                    // TODO: introduce fallback
                }
            }
            if (potentialExc != null) {
                throw potentialExc;
            }
        } else if (dto instanceof final TransitoryMenuSnapshotUpdatedDto transitoryDto) {
            LOGGER.trace("Transitioning to the new page...");

            try {
                if (transitoryDto.getNextPage().getLocalizationFunction() == null) {
                    LOGGER.trace("Sending new message markup...");
                    clientManager.getClient(botRole.getBot()).execute(EditMessageReplyMarkup.builder()
                            .chatId(botRole.getUser().getId())
                            .messageId(transitoryDto.getSnapshot().getMessageId())
                            .replyMarkup(keyboardUtil.getMarkup(transitoryDto.getNextPage(),
                                transitoryDto.getSnapshotButtons(), transitoryDto.getButtons()))
                            .build());
                    LOGGER.trace("New markup sent.");
                    return;
                } else {
                    LOGGER.trace("Sending new message content and markup...");
                    final Localization loc = transitoryDto.getNextPage().getLocalizationFunction()
                            .apply(new MenuParamsDto(botRole, transitoryDto.getParams(), transitoryDto.getSnapshot().getInitialPage()));

                    clientManager.getClient(botRole.getBot()).execute(EditMessageText.builder()
                            .chatId(botRole.getUser().getId())
                            .messageId(transitoryDto.getSnapshot().getMessageId())
                            .replyMarkup(keyboardUtil.getMarkup(transitoryDto.getNextPage(),
                                transitoryDto.getSnapshotButtons(), transitoryDto.getButtons()))
                            .text(loc.getData())
                            .entities(loc.getEntities())
                            .build());
                    LOGGER.trace("New content and markup sent.");
                    return;
                }  
            } catch (TelegramApiException e) {
                LOGGER.error("Unable to update message " + query.getMessage()
                        .getMessageId() + " and user " + botRole.getUser().getId(), e);

                // TODO: introduce fallback
            }
        } else {
            throw new MenuException("An unknown response DTO was returned by the transactional service. This is a bug.", null);
        }
    }
}

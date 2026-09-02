package com.unbidden.telegramcoursesbot.menu;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.unbidden.telegramcoursesbot.dto.internal.MenuParamsDto;
import com.unbidden.telegramcoursesbot.dto.internal.MenuSnapshotCreatedDto;
import com.unbidden.telegramcoursesbot.dto.internal.MenuSnapshotUpdatedDto;
import com.unbidden.telegramcoursesbot.dto.internal.TerminalMenuSnapshotUpdatedDto;
import com.unbidden.telegramcoursesbot.dto.internal.TransitoryMenuSnapshotUpdatedDto;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.MenuException;
import com.unbidden.telegramcoursesbot.exception.handler.StaleMenuException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.model.BackwardMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.TerminalMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.TransitoryMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.repository.MenuSnapshotButtonRepository;
import com.unbidden.telegramcoursesbot.repository.MenuSnapshotRepository;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(MenuService.class);

    private static final String TERMINAL_PARAM_NAME = "terminal";

    private final MenuSnapshotRepository menuSnapshotRepository;

    private final MenuSnapshotButtonRepository menuSnapshotButtonRepository;

    private final MenuRepository menuRepository;
    
    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    @Transactional
    public MenuSnapshotCreatedDto createSnapshot(BotRole botRole, MenuKey key, Integer initialPage,
            List<Button> buttons, Map<String, String> params, @Nullable Integer messageId,
            @Nullable MenuTerminationGroupKey mtgKey, @Nullable Object[] mtgArgs) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(initialPage, "initialPage cannot be null");
        Assert.notNull(buttons, "buttons cannot be null");
        Assert.notEmpty(buttons, "buttons cannot be empty");
        Assert.noNullElements(buttons, "buttons cannot contain null");
        Assert.notNull(params, "params cannot be null");

        final MenuSnapshot snapshot = new MenuSnapshot();

        snapshot.setBotRole(entityUtil.getBotRoleReference(botRole.getId()));
        snapshot.setInitialPage(initialPage);
        snapshot.setCurrentPage(initialPage);
        snapshot.setGroup(mtgKey != null ? mtgKey.getName().formatted(mtgArgs) : null);
        snapshot.setMessageId(messageId);

        snapshot.parseAndSetParams(params);
        snapshot.setKey(key);

        menuSnapshotRepository.save(snapshot);
        final List<MenuSnapshotButton> snapshotButtons = buttons.stream().map(b -> b.toMenuSnapshotButton(snapshot)).toList();

        menuSnapshotButtonRepository.saveAll(snapshotButtons);

        return new MenuSnapshotCreatedDto(snapshot, snapshotButtons);
    }

    @Transactional
    public void addMessageIdToSnapshot(Long snapshotId, Integer messageId) {
        Assert.notNull(snapshotId, "snapshotId cannot be null");
        Assert.notNull(messageId, "messageId cannot be null");

        final Optional<MenuSnapshot> snapshotOpt = menuSnapshotRepository.findById(snapshotId);

        if (snapshotOpt.isPresent()) {
            snapshotOpt.get().setMessageId(messageId);
        } else {
            LOGGER.warn("Failed to assign a message ID to menu snapshot " + snapshotId
                    + " because it doesn't exist.");
        }
    }

    @Transactional
    public MenuSnapshotUpdatedDto processSnapshotUpdate(BotRole botRole, Long snapshotButtonId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(snapshotButtonId, "snapshotButtonId cannot be null");

        final MenuSnapshotButton calledButton = menuSnapshotButtonRepository.findById(snapshotButtonId).orElseThrow(
                () -> new StaleMenuException("Failed to find menu snapshot button " + snapshotButtonId + ".",
                loader.localize(Localizations.Error.STALE_MENU, botRole)));
        final MenuSnapshot snapshot = calledButton.getSnapshot();
        final Menu menu = menuRepository.find(snapshot.getKey()).orElseThrow(() -> new StaleMenuException("Failed to find configured menu "
                + snapshot.getKey() + " specified in snapshot " + snapshot.getId() + ".", loader.localize(Localizations.Error.STALE_MENU, botRole)));
        final Map<String, String> currentParams = snapshot.paramsToMap();
        final List<Integer> pageHistory = snapshot.historyToList();

        if (calledButton instanceof final TransitoryMenuSnapshotButton transitoryButton) {
            LOGGER.trace("Button " + snapshotButtonId + " is a transitory button. Pointer: " + transitoryButton.getPointer() + ".");

            if (transitoryButton.getParamName() != null && transitoryButton.getParamValue() != null) {
                currentParams.put(transitoryButton.getParamName(), transitoryButton.getParamValue());
                snapshot.parseAndSetParams(currentParams);
            }
            pageHistory.add(snapshot.getCurrentPage());
            snapshot.parseAndSetHistory(pageHistory);
            snapshot.setCurrentPage(transitoryButton.getPointer());

            final Page nextPage = menu.getPages().get(transitoryButton.getPointer());
            final List<Button> generatedLayout = nextPage.getButtonsFunction().apply(new MenuParamsDto(botRole, currentParams, snapshot.getInitialPage()));
            final List<MenuSnapshotButton> snapshotButtons = generatedLayout.stream().map(b -> b.toMenuSnapshotButton(snapshot)).toList();
   
            final int numberOfDeletions = menuSnapshotButtonRepository.deleteAllBySnapshotIdInBatch(snapshot.getId());

            LOGGER.trace("Deleted " + numberOfDeletions + " snapshot buttons from menu " + snapshot.getId() + ".");
            menuSnapshotButtonRepository.saveAll(snapshotButtons);

            LOGGER.trace("An iteration of snapshot " + snapshot.getId() + " has been generated.");
            return new TransitoryMenuSnapshotUpdatedDto(snapshot, nextPage, generatedLayout, snapshotButtons, currentParams);
        } else if (calledButton instanceof final TerminalMenuSnapshotButton terminalButton) {
            LOGGER.trace("Button " + snapshotButtonId + " is a terminal button. Handler bean name: " + terminalButton.getHandlerBeanName());
            
            if (terminalButton.getParamValue() != null) {
                if (terminalButton.getParamName() != null) {
                    currentParams.put(terminalButton.getParamName(), terminalButton.getParamValue());
                } else {
                    currentParams.put(TERMINAL_PARAM_NAME, terminalButton.getParamValue());
                }
            }

            if (menu.isResetAfterTerminal() && !snapshot.getCurrentPage().equals(snapshot.getInitialPage())) {
                LOGGER.trace("Menu " + menu.getKey() + " is supposed to be reset after a terminal button call.");
                final Page nextPage = menu.getPages().get(snapshot.getInitialPage());
                final List<Button> generatedLayout = nextPage.getButtonsFunction().apply(new MenuParamsDto(botRole, currentParams, snapshot.getInitialPage()));
                final List<MenuSnapshotButton> snapshotButtons = generatedLayout.stream().map(b -> b.toMenuSnapshotButton(snapshot)).toList();

                snapshot.setCurrentPage(snapshot.getInitialPage());
                snapshot.setPageHistory(null);

                final int numberOfDeletions = menuSnapshotButtonRepository.deleteAllBySnapshotIdInBatch(snapshot.getId());

                LOGGER.trace("Deleted " + numberOfDeletions + " snapshot buttons from snapshot " + snapshot.getId() + ".");
                menuSnapshotButtonRepository.saveAll(snapshotButtons);

                LOGGER.trace("Snapshot " + snapshot.getId() + " has been reset to its original state.");
                return new TerminalMenuSnapshotUpdatedDto(snapshot, nextPage, generatedLayout, snapshotButtons,
                        terminalButton.getHandlerBeanName(), currentParams);
            }
            if (menu.isOneTimeMenu()) {
                LOGGER.trace("Menu " + menu.getKey() + " is supposed to be removed after a terminal button call.");
                final Page terminalPage = menu.getTerminalPage();

                final int numberOfDeletions = menuSnapshotButtonRepository.deleteAllBySnapshotIdInBatch(snapshot.getId());
                
                menuSnapshotRepository.delete(snapshot);
                LOGGER.trace("Deleted " + numberOfDeletions + " snapshot buttons along with their parent snapshot " + snapshot.getId() + ".");

                return new TerminalMenuSnapshotUpdatedDto(snapshot, terminalPage, terminalButton.getHandlerBeanName(), currentParams, true);
            }
            return new TerminalMenuSnapshotUpdatedDto(snapshot, null, terminalButton.getHandlerBeanName(), currentParams, false);
        } else if (calledButton instanceof BackwardMenuSnapshotButton) {
            if (pageHistory.size() == 0) {
                throw new MenuException("Unable to go to the previous page because page history is empty. "
                        + "This is likely a bug, since there shouldn't be any backward buttons if there is nothing to go back to.", null);
            }

            final Page nextPage = menu.getPages().get(pageHistory.removeLast());

            LOGGER.trace("Button " + snapshotButtonId + " is a backward button. Previous page: " + nextPage.getPageIndex());

            snapshot.parseAndSetHistory(pageHistory);
            snapshot.setCurrentPage(nextPage.getPageIndex());

            final List<Button> generatedLayout = nextPage.getButtonsFunction().apply(new MenuParamsDto(botRole, currentParams, snapshot.getInitialPage()));
            final List<MenuSnapshotButton> snapshotButtons = generatedLayout.stream().map(b -> b.toMenuSnapshotButton(snapshot)).toList();
   
            final int numberOfDeletions = menuSnapshotButtonRepository.deleteAllBySnapshotIdInBatch(snapshot.getId());

            LOGGER.trace("Deleted " + numberOfDeletions + " snapshot buttons from menu " + snapshot.getId() + ".");
            menuSnapshotButtonRepository.saveAll(snapshotButtons);

            LOGGER.trace("An iteration of snapshot " + snapshot.getId() + " has been generated.");
            return new TransitoryMenuSnapshotUpdatedDto(snapshot, nextPage, generatedLayout, snapshotButtons, currentParams);
        } else {
            throw new StaleMenuException("A button of unknown type has been called.", loader.localize(Localizations.Error.STALE_MENU, botRole));
        }
    }

    @Transactional
    public MenuSnapshot terminateMenu(BotRole botRole, Long snapshotId) {
        final MenuSnapshot snapshot = menuSnapshotRepository.findById(snapshotId).orElseThrow(() ->
                new EntityNotFoundException("Menu snapshot " + snapshotId + " does not exist.",
                loader.localize(Localizations.Error.MENU_SNAPSHOT_NOT_FOUND, botRole)));

        LOGGER.debug("Deleting menu " + snapshotId + " and its buttons...");
        menuSnapshotButtonRepository.deleteAllBySnapshotIdInBatch(snapshotId);
        menuSnapshotRepository.delete(snapshot);

        return snapshot;
    }

    @Transactional
    public List<MenuSnapshot> terminateMenus(MenuTerminationGroupKey key, Object[] args) {
        final String formattedKey = key.getName().formatted(args);
        final List<MenuSnapshot> snapshots = menuSnapshotRepository.findByGroup(formattedKey);
        final List<Long> ids = snapshots.stream().map(s -> s.getId()).toList();

        LOGGER.debug("Deleting all menus and buttons in group " + formattedKey + "... Snapshot IDs: " + ids);
        menuSnapshotButtonRepository.deleteAllBySnapshotIdsInBatch(ids);
        menuSnapshotRepository.deleteAllByIdInBatch(ids);

        return snapshots;
    }
}

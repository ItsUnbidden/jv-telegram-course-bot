package com.unbidden.telegramcoursesbot.menu.configurer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.menu.handler.SendSupportReplyFeedbackButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.SendSupportRequestFeedbackButtonHandler;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class SupportFeedbackMenu implements MenuConfigurer {
    private final SendSupportRequestFeedbackButtonHandler sendSupportRequestFeedbackHandler;
    private final SendSupportReplyFeedbackButtonHandler sendSupportReplyFeedbackHandler;

    private final SupportOrchestrationService supportService;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.SUPPORT_FEEDBACK);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(2);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.SUPPORT_FEEDBACK_PAGE_0, p.botRole()));
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            final long numberOfNewRequests = supportService.countNewRequestsForUser(p.botRole());
            final long numberOfOldRequests = supportService.countRequestsWithRepliesForUser(p.botRole());

            if (numberOfNewRequests > 0) {
                buttons.add(new TransitoryButton(loader.localize(Localizations.Button.NEW_SUPPORT_REQUESTS, p.botRole(),
                        new Localizations.Button.NewSupportRequestsParams(numberOfNewRequests)).getData(), 1));
            }
            if (numberOfOldRequests > 0) {
                buttons.add(new TransitoryButton(loader.localize(Localizations.Button.OLD_SUPPORT_REQUESTS, p.botRole(),
                        new Localizations.Button.OldSupportRequestsParams(numberOfOldRequests)).getData(), 2));
            }

            return buttons;
        });

        final Page page2 = new Page(menu);

        page2.setPageIndex(1);
        page2.setColumns(2);
        page2.setLocalizationFunction(p -> loader.localize(Localizations.Menu.SUPPORT_FEEDBACK_PAGE_1, p.botRole()));
        page2.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            final List<SupportRequest> requests = supportService.getNewRequestsForUser(p.botRole());

            for (final SupportRequest request : requests) {
                buttons.add(new TerminalButton(request.getUserBotRole().getUser().getFullName(),
                        request.getId().toString(), sendSupportRequestFeedbackHandler));
            }
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page page3 = new Page(menu);

        page3.setPageIndex(2);
        page3.setColumns(2);
        page3.setLocalizationFunction(p -> loader.localize(Localizations.Menu.SUPPORT_FEEDBACK_PAGE_2, p.botRole()));
        page3.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            final List<SupportRequest> requests = supportService.getRequestsWithRepliesForUser(p.botRole());

            for (final SupportRequest request : requests) {
                buttons.add(new TerminalButton(request.getUserBotRole().getUser().getFullName(),
                        request.getId().toString(), sendSupportReplyFeedbackHandler));
            }
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        menu.setPages(List.of(page1, page2, page3));
        menu.setResetAfterTerminal(true);

        return menu;
    }
}

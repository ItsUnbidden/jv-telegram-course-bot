package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.SendLastSupportReplyButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.SendSupportRequestButtonHandler;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportRequestMenu implements MenuConfigurer{
    private final SendSupportRequestButtonHandler sendSupportRequestHandler;
    private final SendLastSupportReplyButtonHandler sendLastSupportReplyHandler;

    private final SupportOrchestrationService supportService;

    private final LocalizationLoader loader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.SUPPORT_REQUEST);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(2);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.SUPPORT_REQUEST_PAGE_0, p.botRole()));
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.SEND_SUPPORT_REQUEST, p.botRole()).getData(), sendSupportRequestHandler));
            if (!supportService.isUserEligibleForSupport(p.botRole())) {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.SEND_LAST_SUPPORT_REPLY, p.botRole()).getData(), sendLastSupportReplyHandler));
            }

            return buttons;
        });

        final Page terminalPage = new Page(menu);

        terminalPage.setPageIndex(1);
        terminalPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.SUPPORT_REQUEST_TERMINAL_PAGE, p.botRole()));

        menu.setTerminalPage(terminalPage);
        menu.setPages(List.of(page1));
        menu.setOneTimeMenu(false);

        return menu;
    }
}

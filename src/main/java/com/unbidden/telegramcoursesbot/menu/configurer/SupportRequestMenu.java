package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.ResolveSupportRequestButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.SendSupportRequestButtonHandler;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportRequestMenu implements MenuConfigurer{
    private static final String REQUEST_ID_PARAM = "requestId";

    private final SendSupportRequestButtonHandler sendSupportRequestHandler;
    private final ResolveSupportRequestButtonHandler resolveRequestHandler;

    private final SupportOrchestrationService supportService;

    private final LocalizationLoader loader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.SUPPORT_REQUEST);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(2);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.SUPPORT_REQUEST_PAGE_0, p.user()));
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                buttons.add(new TerminalButton("support" + i, String.valueOf(i), sendSupportRequestHandler));
            }
            if (!supportService.isUserEligibleForSupport(p.user(), p.bot())) {
                final List<SupportRequest> unresolvedRequests = supportService.getUnresolvedRequestsForUserInBot(p.user(), p.bot());

                buttons.add(new TerminalButton(loader.localize(Localizations.Button.RESOLVE_LAST_SUPPORT_REQUEST, p.user()).getData(),
                        REQUEST_ID_PARAM, unresolvedRequests.get(0).getId().toString(), resolveRequestHandler));
            }

            return buttons;
        });

        final Page terminalPage = new Page(menu);

        terminalPage.setPageIndex(1);
        terminalPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.SUPPORT_REQUEST_TERMINAL_PAGE, p.user()));

        menu.setTerminalPage(terminalPage);
        menu.setPages(List.of(page1));
        menu.setOneTimeMenu(true);

        return menu;
    }
}

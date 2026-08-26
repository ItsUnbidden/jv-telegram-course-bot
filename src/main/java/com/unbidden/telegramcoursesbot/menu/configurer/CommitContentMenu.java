package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.CancelSessionButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CommitSessionButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.ResendSessionButtonHandler;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommitContentMenu implements MenuConfigurer {
    private final CommitSessionButtonHandler commitSessionButtonHandler;
    private final CancelSessionButtonHandler cancelSessionButtonHandler;
    private final ResendSessionButtonHandler resendSessionButtonHandler;
    
    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.COMMIT_CONTENT);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(2);
        page.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COMMIT_CONTENT_PAGE_0, p.user()));
        page.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.CONFIRM_SEND_CONTENT, p.user()).getData(), commitSessionButtonHandler),
            new TerminalButton(loader.localize(Localizations.Button.RESEND_CONTENT, p.user()).getData(), resendSessionButtonHandler),
            new TerminalButton(loader.localize(Localizations.Button.CANCEL_SESSION, p.user()).getData(), cancelSessionButtonHandler)
        ));

        final Page terminalPage = new Page(menu);

        terminalPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COMMIT_CONTENT_TERMINAL_PAGE, p.user()));

        menu.setPages(List.of(page));
        menu.setTerminalPage(terminalPage);

        return menu;
    }
}

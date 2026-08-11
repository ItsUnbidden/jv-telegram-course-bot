package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.CancelSessionButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.CommitSessionButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.ResendSessionButtonHandler;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommitContentMenu implements MenuConfigurer {
    private static final String RESEND_CONTENT = "rsc";
    private static final String CONFIRM_CONTENT = "cc";
    private static final String CANCEL_SESSION = "cs";

    private final CommitSessionButtonHandler commitSessionButtonHandler;
    private final CancelSessionButtonHandler cancelSessionButtonHandler;
    private final ResendSessionButtonHandler resendSessionButtonHandler;
    
    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.COMMIT_CONTENT);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setButtonsRowSize(2);
        page.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.COMMIT_CONTENT_PAGE_0, u));
        page.setButtonsFunction((u, p, b) -> List.of(
                new TerminalButton(loader.localize(Localizations.Button.CONFIRM_SEND_CONTENT, u).getData(), CONFIRM_CONTENT, commitSessionButtonHandler),
                new TerminalButton(loader.localize(Localizations.Button.RESEND_CONTENT, u).getData(), RESEND_CONTENT, resendSessionButtonHandler),
                new TerminalButton(loader.localize(Localizations.Button.CANCEL_SESSION, u).getData(), CANCEL_SESSION, cancelSessionButtonHandler)
        ));

        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(true);

        return menu;
    }
}

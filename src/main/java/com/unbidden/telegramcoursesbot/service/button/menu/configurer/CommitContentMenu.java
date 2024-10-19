package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommitContentMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_cmtCnt";
    
    private static final String RESEND_CONTENT = "rsc";
    private static final String CONFIRM_CONTENT = "cc";
    private static final String CANCEL_SESSION = "cs";

    private static final String BUTTON_RESEND_CONTENT = "button_resend_content";
    private static final String BUTTON_CONFIRM_SEND_CONTENT = "button_confirm_send_content";
    private static final String BUTTON_CANCEL_SESSION = "button_cancel_session";
    
    private static final String MENU_COMMIT_CONTENT_PAGE_0 = "menu_commit_content_page_0";
    private static final String MENU_COMMIT_CONTENT_TERMINAL_PAGE = "menu_commit_content_terminal_page";

    private final LocalizationLoader localizationLoader;

    private final MenuService menuService;

    private final ContentSessionService sessionService;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(2);
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                MENU_COMMIT_CONTENT_PAGE_0, u));
        page.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_CONFIRM_SEND_CONTENT, u)
                .getData(), CONFIRM_CONTENT, (u1, pa) -> sessionService.commit(
                    Integer.parseInt(pa[0]))), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_RESEND_CONTENT, u)
                .getData(), RESEND_CONTENT, (u1, pa) -> sessionService.resend(
                    Integer.parseInt(pa[0]))), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_CANCEL_SESSION, u)
                .getData(), CANCEL_SESSION, (u1, pa) -> sessionService.cancel(
                    Integer.parseInt(pa[0])))));

        final Page terminalPage = new Page();
        terminalPage.setMenu(menu);
        terminalPage.setPageIndex(1);
        terminalPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                MENU_COMMIT_CONTENT_TERMINAL_PAGE, u));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page, terminalPage));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(true);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

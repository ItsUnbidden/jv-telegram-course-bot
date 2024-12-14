package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
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
        page.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
                MENU_COMMIT_CONTENT_PAGE_0, u));
        page.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_CONFIRM_SEND_CONTENT, u)
                .getData(), CONFIRM_CONTENT, (b1, u1, pa) -> sessionService.commit(
                    Integer.parseInt(pa[0]), u1)), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_RESEND_CONTENT, u)
                .getData(), RESEND_CONTENT, (b1, u1, pa) -> sessionService.resend(
                    Integer.parseInt(pa[0]), u1)), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_CANCEL_SESSION, u)
                .getData(), CANCEL_SESSION, (b1, u1, pa) -> sessionService.cancel(
                    Integer.parseInt(pa[0]), u1))));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(true);
        menuService.save(menu);
    }
}

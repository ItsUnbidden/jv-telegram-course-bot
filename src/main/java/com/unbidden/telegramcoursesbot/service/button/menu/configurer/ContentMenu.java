package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.handler.GetContentButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.UploadContentButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_cntAct";

    private static final String UPLOAD_CONTENT = "upC";
    private static final String GET_CONTENT = "gC";

    private static final String BUTTON_GET_CONTENT = "button_get_content";
    private static final String BUTTON_UPLOAD_CONTENT = "button_upload_content";

    private static final String MENU_CONTENT_ACTIONS_PAGE_0 = "menu_content_actions_page_0";

    private final GetContentButtonHandler getContentHandler;
    private final UploadContentButtonHandler uploadContentHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(2);
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_CONTENT_ACTIONS_PAGE_0, u));
        page.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_UPLOAD_CONTENT, u)
                    .getData(), UPLOAD_CONTENT, uploadContentHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_GET_CONTENT, u)
                    .getData(), GET_CONTENT, getContentHandler)));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menuService.save(menu);
    }
}

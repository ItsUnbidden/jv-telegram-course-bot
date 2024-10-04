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
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            "menu_content_actions_page_0", u));
        page.setButtonsFunction(u -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser("button_upload_content", u)
                    .getData(), "upC", uploadContentHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser("button_get_content", u)
                    .getData(), "gC", getContentHandler)));
        menu.setName("m_cntAct");
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setUpdateAfterTerminalButtonRequired(false);
        menuService.save(menu);
    }
}

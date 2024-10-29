package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.handler.UpdateContentButtonHandler;
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
@Deprecated
public class UpdateContentMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_cntUpd";

    private static final String UPDATE_CONTENT = "updC";

    private static final String BUTTON_UPDATE_CONTENT = "button_update_content";

    private final UpdateContentButtonHandler updateContentHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_UPDATE_CONTENT, u)
                    .getData(), UPDATE_CONTENT, updateContentHandler)));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(true);
        menuService.save(menu);
    }
}

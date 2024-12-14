package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.RefreshBotNameAndDescriptionsButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.RefreshLocalizationsButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.RefreshUserMenusButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_rfsh";

    private static final String MENU_REFRESH = "mr";
    private static final String DESC_NAME_REFRESH = "dnr";
    private static final String LOCALIZATIONS_REFRESH = "lr";
    
    private static final String BUTTON_MENUS_REFRESH = "button_menus_refresh";
    private static final String BUTTON_DESC_NAME_REFRESH = "button_desc_name_refresh";
    private static final String BUTTON_LOCALIZATIONS_REFRESH = "button_localizations_refresh";

    private static final String MENU_REFRESH_PAGE_0 = "menu_refresh_page_0";

    private final RefreshBotNameAndDescriptionsButtonHandler refreshBotNameAndDescriptionsHandler;
    private final RefreshLocalizationsButtonHandler refreshLocalizationsHandler;
    private final RefreshUserMenusButtonHandler refreshUserMenusHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page1 = new Page();
        page1.setMenu(menu);
        page1.setPageIndex(0);
        page1.setButtonsRowSize(1);
        page1.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
                MENU_REFRESH_PAGE_0, u));
        page1.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_LOCALIZATIONS_REFRESH, u).getData(), LOCALIZATIONS_REFRESH,
                    refreshLocalizationsHandler));

            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_DESC_NAME_REFRESH, u).getData(), DESC_NAME_REFRESH,
                    refreshBotNameAndDescriptionsHandler));

            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_MENUS_REFRESH, u).getData(), MENU_REFRESH, refreshUserMenusHandler));
            return buttons;
        });
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page1));
        menuService.save(menu);
    }
}

package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.GeneralBanButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneralBanMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_gnrlBn";

    private static final String LIFT_BAN = "lb";
    private static final String GIVE_BAN = "gb";
    private static final String CHOOSE_USER = "chu";
    private static final String BY_ID = "bid";

    private static final String BUTTON_BACK = "button_back";
    private static final String BUTTON_LIFT_BAN = "button_lift_ban";
    private static final String BUTTON_GIVE_BAN = "button_give_ban";
    private static final String BUTTON_CHOOSE_USER = "button_ban_choose_user";
    private static final String BUTTON_BY_ID = "button_ban_by_id";

    private static final String MENU_ADMIN_ACTIONS_PAGE_0 = "menu_general_ban_page_0";
    private static final String MENU_ADMIN_ACTIONS_PAGE_1 = "menu_general_ban_page_1";

    private final GeneralBanButtonHandler generalBanHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page1 = new Page();
        page1.setMenu(menu);
        page1.setPageIndex(0);
        page1.setPreviousPage(0);
        page1.setButtonsRowSize(1);
        page1.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
            MENU_ADMIN_ACTIONS_PAGE_0, u));
        page1.setButtonsFunction((u, p, b) -> List.of(new TransitoryButton(localizationLoader
                .getLocalizationForUser(BUTTON_GIVE_BAN, u).getData(), GIVE_BAN, 1),
                new TransitoryButton(localizationLoader.getLocalizationForUser(BUTTON_LIFT_BAN, u)
                .getData(), LIFT_BAN, 1)));
        final Page page2 = new Page();
        page2.setMenu(menu);
        page2.setPageIndex(1);
        page2.setPreviousPage(0);
        page2.setButtonsRowSize(2);
        page2.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
            MENU_ADMIN_ACTIONS_PAGE_1, u));
        page2.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(localizationLoader
                .getLocalizationForUser(BUTTON_BY_ID, u).getData(), BY_ID, generalBanHandler),
                new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_CHOOSE_USER, u).getData(), CHOOSE_USER, generalBanHandler),
                new BackwardButton(localizationLoader.getLocalizationForUser(BUTTON_BACK, u)
                .getData())));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page1, page2));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setUpdateAfterTerminalButtonRequired(false);
        menuService.save(menu);
    }
}

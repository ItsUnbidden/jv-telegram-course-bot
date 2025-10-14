package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.CreateBotButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.DisableBotButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.ListBotsButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_bots";

    private static final String LIST_BOTS = "lb";
    private static final String DISABLE_BOT = "db";
    private static final String CREATE_BOT = "cb";
    private static final String CHOOSE_USER = "chu";
    private static final String BY_ID = "bid";

    private static final String BUTTON_LIST_BOTS = "button_list_bots";
    private static final String BUTTON_DISABLE_BOT = "button_disable_bot";
    private static final String BUTTON_CREATE_BOT = "button_create_bot";
    private static final String BUTTON_CHOOSE_USER = "button_choose_user";
    private static final String BUTTON_BY_ID = "button_by_id";
    private static final String BUTTON_BACK = "button_back";

    private static final String MENU_BOT_PAGE_0 = "menu_bot_page_0";
    private static final String MENU_BOT_PAGE_1 = "menu_bot_page_1";

    private final CreateBotButtonHandler createBotHandler;
    private final DisableBotButtonHandler disableBotHandler;
    private final ListBotsButtonHandler listBotsHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;
    
    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page1 = new Page();
        page1.setMenu(menu);
        page1.setPageIndex(0);
        page1.setButtonsRowSize(2);
        page1.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
                MENU_BOT_PAGE_0, u));
        page1.setButtonsFunction((u, p, b) -> List.of(new TransitoryButton(
                localizationLoader.getLocalizationForUser(BUTTON_CREATE_BOT, u)
                    .getData(), CREATE_BOT, 1), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_DISABLE_BOT, u)
                    .getData(), DISABLE_BOT, disableBotHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_LIST_BOTS, u)
                    .getData(), LIST_BOTS, listBotsHandler)));
        final Page page2 = new Page();
        page2.setMenu(menu);
        page2.setPageIndex(1);
        page2.setPreviousPage(0);
        page2.setButtonsRowSize(2);
        page2.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
                MENU_BOT_PAGE_1, u));
        page2.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_BY_ID, u)
                    .getData(), BY_ID, createBotHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_CHOOSE_USER, u)
                    .getData(), CHOOSE_USER, createBotHandler), new BackwardButton(
                localizationLoader.getLocalizationForUser(BUTTON_BACK, u)
                    .getData())));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page1, page2));
        menuService.save(menu);
    }
}

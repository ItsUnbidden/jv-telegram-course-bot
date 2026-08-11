package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.CreateBotButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.DisableBotButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.ListBotsButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotMenu implements MenuConfigurer {
    private static final String LIST_BOTS = "lb";
    private static final String DISABLE_BOT = "db";
    private static final String CREATE_BOT = "cb";
    private static final String CHOOSE_USER = "chu";
    private static final String BY_ID = "bid";

    private final CreateBotButtonHandler createBotHandler;
    private final DisableBotButtonHandler disableBotHandler;
    private final ListBotsButtonHandler listBotsHandler;

    private final LocalizationLoader loader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.BOT);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setButtonsRowSize(2);
        page1.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.BOT_PAGE_0, u));
        page1.setButtonsFunction((u, p, b) -> List.of(new TransitoryButton(
                loader.localize(Localizations.Button.CREATE_BOT, u)
                    .getData(), CREATE_BOT, 1), new TerminalButton(
                loader.localize(Localizations.Button.DISABLE_BOT, u)
                    .getData(), DISABLE_BOT, disableBotHandler), new TerminalButton(
                loader.localize(Localizations.Button.LIST_BOTS, u)
                    .getData(), LIST_BOTS, listBotsHandler)));
        final Page page2 = new Page(menu);

        page2.setPageIndex(1);
        page2.setPreviousPage(0);
        page2.setButtonsRowSize(2);
        page2.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.BOT_PAGE_1, u));
        page2.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(
                loader.localize(Localizations.Button.BY_ID, u)
                    .getData(), BY_ID, createBotHandler), new TerminalButton(
                loader.localize(Localizations.Button.CHOOSE_USER, u)
                    .getData(), CHOOSE_USER, createBotHandler), new BackwardButton(
                loader.localize(Localizations.Button.BACK, u)
                    .getData())));

        menu.setPages(List.of(page1, page2));

        return menu;
    }
}

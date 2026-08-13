package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.menu.handler.CreateBotButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.DisableBotButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.ListBotsButtonHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotMenu implements MenuConfigurer {
    private final CreateBotButtonHandler createBotHandler;
    private final DisableBotButtonHandler disableBotHandler;
    private final ListBotsButtonHandler listBotsHandler;

    private final LocalizationLoader loader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.BOT);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(2);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.BOT_PAGE_0, p.user()));
        page1.setButtonsFunction(p -> List.of(
            new TransitoryButton(loader.localize(Localizations.Button.CREATE_BOT, p.user()).getData(), 1),
            new TerminalButton(loader.localize(Localizations.Button.DISABLE_BOT, p.user()).getData(), disableBotHandler),
            new TerminalButton(loader.localize(Localizations.Button.LIST_BOTS, p.user()).getData(), listBotsHandler)
        ));
        final Page page2 = new Page(menu);

        page2.setPageIndex(1);
        page2.setColumns(2);
        page2.setLocalizationFunction(p -> loader.localize(Localizations.Menu.BOT_PAGE_1, p.user()));
        page2.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.BY_ID, p.user()).getData(), createBotHandler),
            new TerminalButton(loader.localize(Localizations.Button.CHOOSE_USER, p.user()).getData(), createBotHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.user()).getData())
        ));

        menu.setPages(List.of(page1, page2));

        return menu;
    }
}

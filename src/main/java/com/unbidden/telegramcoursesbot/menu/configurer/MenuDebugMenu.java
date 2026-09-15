package com.unbidden.telegramcoursesbot.menu.configurer;

import java.util.List;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.TerminateMenuButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.TerminateMenusForUserInBotButtonHandler;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class MenuDebugMenu implements MenuConfigurer {
    private final TerminateMenuButtonHandler terminateMenuHandler;
    private final TerminateMenusForUserInBotButtonHandler terminateMenusForUserInBotHandler;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.MENU_DEBUG);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(2);
        page.setLocalizationFunction(p -> loader.localize(Localizations.Menu.MENU_DEBUG_PAGE_0, p.botRole()));
        page.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.TERMINATE_MENU, p.botRole()).getData(), terminateMenuHandler),
            new TerminalButton(loader.localize(Localizations.Button.TERMINATE_MENUS_FOR_USER_IN_BOT, p.botRole()).getData(), terminateMenusForUserInBotHandler)
        ));

        menu.setPages(List.of(page));

        return menu;
    }
}

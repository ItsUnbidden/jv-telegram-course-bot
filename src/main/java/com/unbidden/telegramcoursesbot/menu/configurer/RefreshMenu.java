package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.RefreshLocalizationsButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RefreshUserMenusButtonHandler;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshMenu implements MenuConfigurer {
    private final RefreshLocalizationsButtonHandler refreshLocalizationsHandler;
    private final RefreshUserMenusButtonHandler refreshUserMenusHandler;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.REFRESH);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(1);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.REFRESH_PAGE_0, p.botRole()));
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.LOCALIZATIONS_REFRESH, p.botRole()).getData(), refreshLocalizationsHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.MENUS_REFRESH, p.botRole()).getData(), refreshUserMenusHandler));

            return buttons;
        });

        menu.setPages(List.of(page1));

        return menu;
    }
}

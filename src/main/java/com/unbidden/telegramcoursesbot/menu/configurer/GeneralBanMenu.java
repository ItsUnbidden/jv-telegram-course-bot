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
import com.unbidden.telegramcoursesbot.menu.handler.GeneralBanButtonHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneralBanMenu implements MenuConfigurer {
    private static final String IS_BY_ID_PARAM = "isById";
    private static final String IS_GIVE_BAN_PARAM = "isGiveBan";

    private final GeneralBanButtonHandler generalBanHandler;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.GENERAL_BAN);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(1);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.GENERAL_BAN_PAGE_0, p.user()));
        page1.setButtonsFunction(p -> List.of(
            new TransitoryButton(loader.localize(Localizations.Button.GIVE_BAN, p.user()).getData(),
                IS_GIVE_BAN_PARAM, String.valueOf(true), 1),
            new TransitoryButton(loader.localize(Localizations.Button.LIFT_BAN, p.user()).getData(),
                IS_GIVE_BAN_PARAM, String.valueOf(false), 1)));

        final Page page2 = new Page(menu);
;
        page2.setPageIndex(1);
        page2.setColumns(2);
        page2.setLocalizationFunction(p -> loader.localize(Localizations.Menu.GENERAL_BAN_PAGE_0, p.user()));
        page2.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.BY_ID, p.user()).getData(),
                IS_BY_ID_PARAM, String.valueOf(true), generalBanHandler),
            new TerminalButton(loader.localize(Localizations.Button.CHOOSE_USER, p.user()).getData(),
                IS_BY_ID_PARAM, String.valueOf(false), generalBanHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.user()).getData())));

        menu.setPages(List.of(page1, page2));

        menu.setResetAfterTerminal(true);

        return menu;
    }
}

package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.GeneralPostButtonHandler;
import com.unbidden.telegramcoursesbot.model.RoleType;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneralPostMenu implements MenuConfigurer {
    private final GeneralPostButtonHandler generalPostHandler;

    private final LocalizationLoader localizationLoader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.GENERAL_POST);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(3);
        page1.setLocalizationFunction(p -> localizationLoader.localize(Localizations.Menu.GENERAL_POST_PAGE_0, p.botRole()));
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            for (final RoleType roleType : RoleType.values()) {
                buttons.add(new TerminalButton(roleType.toString(), roleType.toString(), generalPostHandler));
            }
            buttons.add(new TerminalButton(localizationLoader.localize(Localizations.Button.POST_CUSTOM_ROLE_SET, p.botRole()).getData(), generalPostHandler));

            return buttons;
        });

        menu.setPages(List.of(page1));

        return menu;
    }
}

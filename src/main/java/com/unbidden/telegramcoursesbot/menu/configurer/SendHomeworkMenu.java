package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.SendHomeworkButtonHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendHomeworkMenu implements MenuConfigurer {
    private final SendHomeworkButtonHandler sendHomeworkHandler;

    private final LocalizationLoader localizationLoader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.SEND_HOMEWORK);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(1);
        page.setButtonsFunction(p -> List.of(
            new TerminalButton(localizationLoader.localize(Localizations.Button.SEND_HOMEWORK, p.user()).getData(), sendHomeworkHandler)
        ));

        menu.setPages(List.of(page));

        return menu;
    }
}

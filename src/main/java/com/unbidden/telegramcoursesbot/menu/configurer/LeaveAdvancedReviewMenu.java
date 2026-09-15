package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.SendAdvancedReviewButtonHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveAdvancedReviewMenu implements MenuConfigurer {
    private final SendAdvancedReviewButtonHandler sendAdvancedReviewHandler;

    private final LocalizationLoader loader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.LEAVE_ADVANCED_REVIEW);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(1);
        page.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.SEND_ADVANCED_REVIEW, p.botRole()).getData(), sendAdvancedReviewHandler)
        ));

        menu.setPages(List.of(page));

        return menu;
    }
}

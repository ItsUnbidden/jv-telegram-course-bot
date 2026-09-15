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
import com.unbidden.telegramcoursesbot.menu.handler.AcceptHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.DeclineHomeworkButtonHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestFeedbackMenu implements MenuConfigurer {
    private static final String WITH_COMMENT_PARAM = "withComment";

    private final AcceptHomeworkButtonHandler acceptHandler;
    private final DeclineHomeworkButtonHandler declineHandler;

    private final LocalizationLoader localizationLoader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.REQUEST_FEEDBACK);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(1);
        page1.setButtonsFunction(p -> List.of(
            new TerminalButton(localizationLoader.localize(Localizations.Button.DECLINE_HOMEWORK, p.botRole()).getData(), declineHandler),
            new TransitoryButton(localizationLoader.localize(Localizations.Button.GENERAL_ACCEPT_HOMEWORK, p.botRole()).getData(), 1)
        ));
                
        final Page page2 = new Page(menu);

        page2.setPageIndex(1);
        page2.setColumns(1);
        page2.setButtonsFunction(p -> List.of(
            new TerminalButton(localizationLoader.localize(Localizations.Button.ACCEPT_HOMEWORK, p.botRole()).getData(),
                    WITH_COMMENT_PARAM, String.valueOf(false), acceptHandler),
            new TerminalButton(localizationLoader.localize(Localizations.Button.ACCEPT_HOMEWORK_WITH_COMMENT, p.botRole()).getData(),
                    WITH_COMMENT_PARAM, String.valueOf(true), acceptHandler),
            new BackwardButton(localizationLoader.localize(Localizations.Button.BACK, p.botRole()).getData())
        ));

        menu.setPages(List.of(page1, page2));
        
        return menu;
    }
}

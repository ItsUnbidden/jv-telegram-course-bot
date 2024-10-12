package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.handler.AcceptHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.DeclineHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestFeedbackMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_rqF";

    private static final String ACCEPT_WITH_COMMENT = "ahwc";
    private static final String ACCEPT = "ah";
    private static final String GENERAL_ACCEPT = "gah";
    private static final String DECLINE = "dh";

    private static final String BUTTON_ACCEPT_HOMEWORK_WITH_COMMENT = "button_accept_homework_with_comment";
    private static final String BUTTON_ACCEPT_HOMEWORK = "button_accept_homework";
    private static final String BUTTON_GENERAL_ACCEPT_HOMEWORK = "button_general_accept_homework";
    private static final String BUTTON_DECLINE_HOMEWORK = "button_decline_homework";
    
    private final AcceptHomeworkButtonHandler acceptHandler;
    private final DeclineHomeworkButtonHandler declineHandler;

    private final LocalizationLoader localizationLoader;

    private final MenuService menuService;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page1 = new Page();
        page1.setMenu(menu);
        page1.setPageIndex(0);
        page1.setButtonsRowSize(2);
        page1.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_DECLINE_HOMEWORK, u)
                .getData(), DECLINE, declineHandler), new TransitoryButton(
                localizationLoader.getLocalizationForUser(BUTTON_GENERAL_ACCEPT_HOMEWORK, u)
                .getData(), GENERAL_ACCEPT, 1)));
        final Page page2 = new Page();
        page2.setMenu(menu);
        page2.setPageIndex(1);
        page2.setButtonsRowSize(2);
        page2.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_ACCEPT_HOMEWORK, u)
                .getData(), ACCEPT, acceptHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_ACCEPT_HOMEWORK_WITH_COMMENT, u)
                .getData(), ACCEPT_WITH_COMMENT, acceptHandler)));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page1, page2));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(true);
        menuService.save(menu);
    }
}

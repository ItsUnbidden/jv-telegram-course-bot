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
        page1.setButtonsFunction(u -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser("button_decline_homework", u)
                .getData(), "dh", declineHandler), new TransitoryButton(
                localizationLoader.getLocalizationForUser("button_general_accept_homework", u)
                .getData(), "gah", 1)));
        final Page page2 = new Page();
        page2.setMenu(menu);
        page2.setPageIndex(1);
        page2.setButtonsFunction(u -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser("button_accept_homework", u)
                .getData(), "ah", acceptHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser("button_accept_homework_with"
                + "_comment", u).getData(), "ahwc", acceptHandler)));
        menu.setName("m_rqF");
        menu.setPages(List.of(page1, page2));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(false);
        menu.setUpdateAfterTerminalButtonRequired(false);
        menu.setAttachedToMessage(true);
        menuService.save(menu);
    }
}

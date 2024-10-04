package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.handler.ConfirmSendHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.SendHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfirmSendHomeworkMenu implements MenuConfigurer {
    private final ConfirmSendHomeworkButtonHandler confirmHomeworkHandler;
    private final SendHomeworkButtonHandler sendHomeworkHandler;

    private final LocalizationLoader localizationLoader;

    private final MenuService menuService;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                "menu_confirm_send_homework_page_0", u));
        page.setButtonsFunction(u -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser("button_confirm_send_homework", u)
                .getData(), "ch", confirmHomeworkHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser("button_resend_homework", u)
                .getData(), "rsh", sendHomeworkHandler)));

        final Page terminalPage = new Page();
        terminalPage.setMenu(menu);
        terminalPage.setPageIndex(1);
        terminalPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                "menu_confirm_send_homework_terminal_page", u));
        menu.setName("m_csHw");
        menu.setPages(List.of(page, terminalPage));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(true);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

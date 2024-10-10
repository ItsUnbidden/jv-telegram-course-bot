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
    private static final String MENU_NAME = "m_csHw";
    
    private static final String RESEND_HOMEWORK = "rsh";
    private static final String CONFIRM_HOMEWORK = "ch";

    private static final String BUTTON_RESEND_HOMEWORK = "button_resend_homework";
    private static final String BUTTON_CONFIRM_SEND_HOMEWORK = "button_confirm_send_homework";
    
    private static final String MENU_CONFIRM_SEND_HOMEWORK_PAGE_0 = "menu_confirm_send_homework_page_0";
    private static final String MENU_CONFIRM_SEND_HOMEWORK_TERMINAL_PAGE = "menu_confirm_send_homework_terminal_page";

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
        page.setButtonsRowSize(2);
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                MENU_CONFIRM_SEND_HOMEWORK_PAGE_0, u));
        page.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_CONFIRM_SEND_HOMEWORK, u)
                .getData(), CONFIRM_HOMEWORK, confirmHomeworkHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_RESEND_HOMEWORK, u)
                .getData(), RESEND_HOMEWORK, sendHomeworkHandler)));

        final Page terminalPage = new Page();
        terminalPage.setMenu(menu);
        terminalPage.setPageIndex(1);
        terminalPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                MENU_CONFIRM_SEND_HOMEWORK_TERMINAL_PAGE, u));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page, terminalPage));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(true);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

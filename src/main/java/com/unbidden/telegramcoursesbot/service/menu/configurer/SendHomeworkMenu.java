package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.SendHomeworkButtonHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendHomeworkMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_sHw";

    private static final String SEND_HOMEWORK = "sh";
    
    private static final String BUTTON_SEND_HOMEWORK = "button_send_homework";

    private final SendHomeworkButtonHandler sendHomeworkHandler;

    private final LocalizationLoader localizationLoader;

    private final MenuService menuService;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_SEND_HOMEWORK, u)
                .getData(), SEND_HOMEWORK, sendHomeworkHandler)));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(true);
        menuService.save(menu);
    }
}

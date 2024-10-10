package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.handler.SendAdvancedReviewButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveAdvancedReviewMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_laR";

    private static final String SEND_ADVANCED_REVIEW = "saR";

    private static final String BUTTON_SEND_ADVANCED_REVIEW = "button_send_advanced_review";

    private final SendAdvancedReviewButtonHandler sendAdvancedReviewHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;
    
    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_SEND_ADVANCED_REVIEW, u)
                .getData(), SEND_ADVANCED_REVIEW, sendAdvancedReviewHandler)));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(true);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.handler.GeneralPostButtonHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneralPostMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_gnrlPst";
    
    private static final String POST_CUSTOM_ROLE_SET = "pcrs";

    private static final String MENU_GENERAL_POST_PAGE_0 = "menu_general_post_page_0";
    
    private static final String BUTTON_POST_CUSTOM_ROLE_SET = "button_post_custom_role_set";

    private final GeneralPostButtonHandler generalPostHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page1 = new Page();
        page1.setMenu(menu);
        page1.setPageIndex(0);
        page1.setButtonsRowSize(3);
        page1.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
                MENU_GENERAL_POST_PAGE_0, u));
        page1.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            for (RoleType roleType : RoleType.values()) {
                buttons.add(new TerminalButton(roleType.toString(), roleType.toString(),
                        generalPostHandler));
            }
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_POST_CUSTOM_ROLE_SET, u).getData(),
                    POST_CUSTOM_ROLE_SET, generalPostHandler));
            return buttons;
        });
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page1));
        menuService.save(menu);
    }
}

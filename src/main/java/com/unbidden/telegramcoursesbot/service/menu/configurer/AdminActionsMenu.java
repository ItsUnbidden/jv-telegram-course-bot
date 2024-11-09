package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.AddOrRemoveAdminButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.ListAdminsButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.ReceiveHomeworkToggleButtonHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminActionsMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_admAct";
    
    private static final String TOGGLE_RECEIVE_HOMEWORK = "rh";
    private static final String LIST_ADMINS = "lA";
    private static final String ADD_OR_REMOVE_ADMIN = "arA";

    private static final String BUTTON_TOGGLE_RECEIVE_HOMEWORK = "button_toggle_receive_homework";
    private static final String BUTTON_LIST_ADMINS = "button_list_admins";
    private static final String BUTTON_ADD_OR_REMOVE_ADMIN = "button_add_or_remove_admin";

    private static final String MENU_ADMIN_ACTIONS_PAGE_0 = "menu_admin_actions_page_0";

    private final AddOrRemoveAdminButtonHandler addOrRemoveAdminHandler;
    private final ListAdminsButtonHandler listAdminsHandler;
    private final ReceiveHomeworkToggleButtonHandler receiveHomeworkHandler;

    private final LocalizationLoader localizationLoader;

    private final MenuService menuService;

    @Override
    public void configure() {
        final Menu adminActionsMenu = new Menu();
        final Page page = new Page();
        page.setMenu(adminActionsMenu);
        page.setPageIndex(0);
        page.setButtonsRowSize(2);
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_ADMIN_ACTIONS_PAGE_0, u));
        page.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_ADD_OR_REMOVE_ADMIN, u)
                    .getData(), ADD_OR_REMOVE_ADMIN, addOrRemoveAdminHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_LIST_ADMINS, u)
                    .getData(), LIST_ADMINS, listAdminsHandler),  new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_TOGGLE_RECEIVE_HOMEWORK, u)
                    .getData(), TOGGLE_RECEIVE_HOMEWORK, receiveHomeworkHandler)));
        adminActionsMenu.setName(MENU_NAME);
        adminActionsMenu.setPages(List.of(page));
        adminActionsMenu.setInitialParameterPresent(false);
        adminActionsMenu.setOneTimeMenu(false);
        menuService.save(adminActionsMenu);
    }
}

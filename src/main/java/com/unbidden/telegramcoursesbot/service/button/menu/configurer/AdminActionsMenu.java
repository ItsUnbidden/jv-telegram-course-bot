package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.handler.AddOrRemoveAdminButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.ListAdminsButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.ReceiveHomeworkToggleButtonHandler;
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
public class AdminActionsMenu implements MenuConfigurer {
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
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            "menu_admin_actions_page_0", u));
        page.setButtonsFunction(u -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser("button_add_or_remove_admin", u)
                    .getData(), "arA", addOrRemoveAdminHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser("button_list_admins", u)
                    .getData(), "lA", listAdminsHandler),  new TerminalButton(
                localizationLoader.getLocalizationForUser("button_toggle_receive_homework", u)
                    .getData(), "rh", receiveHomeworkHandler)));
        adminActionsMenu.setName("m_admAct");
        adminActionsMenu.setPages(List.of(page));
        adminActionsMenu.setInitialParameterPresent(false);
        adminActionsMenu.setOneTimeMenu(false);
        adminActionsMenu.setUpdateAfterTerminalButtonRequired(false);
        menuService.save(adminActionsMenu);
    }
}

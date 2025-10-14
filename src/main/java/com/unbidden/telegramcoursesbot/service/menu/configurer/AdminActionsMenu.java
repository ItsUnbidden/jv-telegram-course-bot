package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.SetRoleButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.BanButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.ListAdminsButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.ReceiveHomeworkToggleButtonHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminActionsMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_admAct";
    
    private static final String BAN = "ban";
    private static final String TOGGLE_RECEIVE_HOMEWORK = "rh";
    private static final String LIST_ADMINS = "lA";
    private static final String SET_ROLE = "sr";
    private static final String LIFT_BAN = "lb";
    private static final String GIVE_BAN = "gb";
    private static final String CHOOSE_USER = "chu";
    private static final String BY_ID = "bid";
    
    private static final String BUTTON_BAN_OPTIONS = "button_ban_options";
    private static final String BUTTON_TOGGLE_RECEIVE_HOMEWORK = "button_toggle_receive_homework";
    private static final String BUTTON_LIST_ADMINS = "button_list_admins";
    private static final String BUTTON_ADD_OR_REMOVE_ADMIN = "button_set_role";
    private static final String BUTTON_BACK = "button_back";
    private static final String BUTTON_LIFT_BAN = "button_lift_ban";
    private static final String BUTTON_GIVE_BAN = "button_give_ban";
    private static final String BUTTON_CHOOSE_USER = "button_choose_user";
    private static final String BUTTON_BY_ID = "button_by_id";

    private static final String MENU_ADMIN_ACTIONS_PAGE_0 = "menu_admin_actions_page_0";
    private static final String MENU_ADMIN_ACTIONS_PAGE_1 = "menu_admin_actions_page_1";
    private static final String MENU_ADMIN_ACTIONS_PAGE_2 = "menu_admin_actions_page_2";
    private static final String MENU_ADMIN_ACTIONS_PAGE_3 = "menu_admin_actions_page_3";
    
    private final SetRoleButtonHandler setRoleHandler;
    private final ListAdminsButtonHandler listAdminsHandler;
    private final ReceiveHomeworkToggleButtonHandler receiveHomeworkHandler;
    private final BanButtonHandler banHandler;

    private final LocalizationLoader localizationLoader;

    private final MenuService menuService;

    @Override
    public void configure() {
        final Menu adminActionsMenu = new Menu();
        final Page page1 = new Page();
        page1.setMenu(adminActionsMenu);
        page1.setPageIndex(0);
        page1.setButtonsRowSize(2);
        page1.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
            MENU_ADMIN_ACTIONS_PAGE_0, u));
        page1.setButtonsFunction((u, p, b) -> List.of(new TransitoryButton(
                localizationLoader.getLocalizationForUser(BUTTON_ADD_OR_REMOVE_ADMIN, u)
                    .getData(), SET_ROLE, 1), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_LIST_ADMINS, u)
                    .getData(), LIST_ADMINS, listAdminsHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_TOGGLE_RECEIVE_HOMEWORK, u)
                    .getData(), TOGGLE_RECEIVE_HOMEWORK, receiveHomeworkHandler),
                new TransitoryButton(localizationLoader.getLocalizationForUser(
                    BUTTON_BAN_OPTIONS, u).getData(), BAN, 2)));
        final Page page2 = new Page();
        page2.setMenu(adminActionsMenu);
        page2.setPageIndex(1);
        page2.setPreviousPage(0);
        page2.setButtonsRowSize(2);
        page2.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
            MENU_ADMIN_ACTIONS_PAGE_1, u));
        page2.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            buttons.addAll(Arrays.stream(RoleType.values())
                    .filter(rt -> !rt.equals(RoleType.DIRECTOR)
                        && !rt.equals(RoleType.CREATOR)
                        && !rt.equals(RoleType.BANNED))
                    .map(rt -> new TerminalButton(rt.toString(),
                        rt.toString(), setRoleHandler)).toList());
            buttons.add(new BackwardButton(localizationLoader.getLocalizationForUser(BUTTON_BACK,
                    u).getData()));
            return buttons;
        });
        final Page page3 = new Page();
        page3.setMenu(adminActionsMenu);
        page3.setPageIndex(2);
        page3.setPreviousPage(0);
        page3.setButtonsRowSize(1);
        page3.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
            MENU_ADMIN_ACTIONS_PAGE_2, u));
        page3.setButtonsFunction((u, p, b) -> List.of(new TransitoryButton(localizationLoader
                .getLocalizationForUser(BUTTON_GIVE_BAN, u).getData(), GIVE_BAN, 3),
                new TransitoryButton(localizationLoader.getLocalizationForUser(BUTTON_LIFT_BAN, u)
                .getData(), LIFT_BAN, 3), new BackwardButton(localizationLoader
                .getLocalizationForUser(BUTTON_BACK, u).getData())));
        final Page page4 = new Page();
        page4.setMenu(adminActionsMenu);
        page4.setPageIndex(3);
        page4.setPreviousPage(2);
        page4.setButtonsRowSize(2);
        page4.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
            MENU_ADMIN_ACTIONS_PAGE_3, u));
        page4.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(localizationLoader
                .getLocalizationForUser(BUTTON_BY_ID, u).getData(), BY_ID, banHandler),
                new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_CHOOSE_USER, u).getData(), CHOOSE_USER, banHandler),
                new BackwardButton(localizationLoader.getLocalizationForUser(BUTTON_BACK, u)
                .getData())));
        adminActionsMenu.setName(MENU_NAME);
        adminActionsMenu.setPages(List.of(page1, page2, page3, page4));
        adminActionsMenu.setInitialParameterPresent(false);
        adminActionsMenu.setOneTimeMenu(false);
        adminActionsMenu.setUpdateAfterTerminalButtonRequired(false);
        menuService.save(adminActionsMenu);
    }
}

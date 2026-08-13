package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.menu.handler.BanButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.ListAdminsButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.ReceiveHomeworkToggleButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.SetRoleButtonHandler;
import com.unbidden.telegramcoursesbot.model.RoleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminActionsMenu implements MenuConfigurer {
    private final SetRoleButtonHandler setRoleHandler;
    private final ListAdminsButtonHandler listAdminsHandler;
    private final ReceiveHomeworkToggleButtonHandler receiveHomeworkHandler;
    private final BanButtonHandler banHandler;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu adminActionsMenu = new Menu(MenuKey.ADMIN_ACTIONS);

        final Page page1 = new Page(adminActionsMenu);

        page1.setPageIndex(0);
        page1.setColumns(2);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_0, p.user()));
        page1.setButtonsFunction(p -> List.of(
            new TransitoryButton(loader.localize(Localizations.Button.ADD_OR_REMOVE_ADMIN, p.user()).getData(), 1),
            new TerminalButton(loader.localize(Localizations.Button.LIST_ADMINS, p.user()).getData(), listAdminsHandler),
            new TerminalButton(loader.localize(Localizations.Button.TOGGLE_RECEIVE_HOMEWORK, p.user()).getData(), receiveHomeworkHandler),
            new TransitoryButton(loader.localize(Localizations.Button.BAN_OPTIONS, p.user()).getData(), 2)
        ));
        final Page page2 = new Page(adminActionsMenu);

        page2.setPageIndex(1);
        page2.setColumns(2);
        page2.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_1, p.user()));
        page2.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(Arrays.stream(RoleType.values())
                .filter(rt -> !rt.equals(RoleType.DIRECTOR)
                    && !rt.equals(RoleType.CREATOR)
                    && !rt.equals(RoleType.BANNED))
                .map(rt -> new TerminalButton(rt.toString(), rt.toString(), setRoleHandler)).toList());
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.user()).getData()));
            
            return buttons;
        });
        final Page page3 = new Page(adminActionsMenu);

        page3.setPageIndex(2);
        page3.setColumns(1);
        page3.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_2, p.user()));
        page3.setButtonsFunction(p -> List.of(
            new TransitoryButton(loader.localize(Localizations.Button.GIVE_BAN, p.user()).getData(), 3),
            new TransitoryButton(loader.localize(Localizations.Button.LIFT_BAN, p.user()).getData(), 3),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.user()).getData())
        ));
        final Page page4 = new Page(adminActionsMenu);

        page4.setPageIndex(3);
        page4.setColumns(2);
        page4.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_3, p.user()));
        page4.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.BY_ID, p.user()).getData(), banHandler),
            new TerminalButton(loader.localize(Localizations.Button.CHOOSE_USER, p.user()).getData(), banHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.user()).getData())
        ));

        adminActionsMenu.setPages(List.of(page1, page2, page3, page4));

        return adminActionsMenu;
    }
}

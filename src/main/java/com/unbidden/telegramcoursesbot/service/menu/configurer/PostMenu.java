package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.handler.PostButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.SendMessageToUserByIdButtonHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_pst";

    private static final String POST_CUSTOM_ROLE_SET = "pcrs";
    private static final String SEND_PRIVATE_MESSAGE = "spm";
    private static final String POST_OPTIONS = "po";

    private static final String MENU_POST_PAGE_0 = "menu_post_page_0";
    private static final String MENU_POST_PAGE_1 = "menu_post_page_1";

    private static final String BUTTON_POST_CUSTOM_ROLE_SET = "button_post_custom_role_set";
    private static final String BUTTON_SEND_PRIVATE_MESSAGE = "button_send_private_message";
    private static final String BUTTON_POST_OPTIONS = "button_post_options";
    private static final String BUTTON_BACK = "button_back";

    private final PostButtonHandler postButtonHandler;
    private final SendMessageToUserByIdButtonHandler sendMessageToUserByIdHandler;

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
                MENU_POST_PAGE_0, u));
        page1.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TransitoryButton(localizationLoader.getLocalizationForUser(
                    BUTTON_POST_OPTIONS, u).getData(), POST_OPTIONS, 1));

            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_SEND_PRIVATE_MESSAGE, u).getData(), SEND_PRIVATE_MESSAGE,
                sendMessageToUserByIdHandler));
            return buttons;
        });
        final Page page2 = new Page();

        page2.setMenu(menu);
        page2.setPageIndex(1);
        page2.setPreviousPage(0);
        page2.setButtonsRowSize(3);
        page2.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
                MENU_POST_PAGE_1, u));
        page2.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            for (RoleType roleType : RoleType.values()) {
                buttons.add(new TerminalButton(roleType.toString(), roleType.toString(),
                        postButtonHandler));
            }
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_POST_CUSTOM_ROLE_SET, u).getData(),
                    POST_CUSTOM_ROLE_SET, postButtonHandler));
            buttons.add(new BackwardButton(localizationLoader.getLocalizationForUser(
                    BUTTON_BACK, u).getData()));
            return buttons;
        });
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page1, page2));
        menuService.save(menu);
    }
}

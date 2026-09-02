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
import com.unbidden.telegramcoursesbot.menu.handler.PostButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.SendMessageToUserByIdButtonHandler;
import com.unbidden.telegramcoursesbot.model.RoleType;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostMenu implements MenuConfigurer {
    private final PostButtonHandler postButtonHandler;
    private final SendMessageToUserByIdButtonHandler sendMessageToUserByIdHandler;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.POST);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(2);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.POST_PAGE_0, p.botRole()));
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.POST_OPTIONS, p.botRole()).getData(), 1));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.SEND_PRIVATE_MESSAGE, p.botRole()).getData(), sendMessageToUserByIdHandler));

            return buttons;
        });

        final Page page2 = new Page(menu);

        page2.setPageIndex(1);
        page2.setColumns(3);
        page2.setLocalizationFunction(p -> loader.localize(Localizations.Menu.POST_PAGE_1, p.botRole()));
        page2.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            for (final RoleType roleType : RoleType.values()) {
                buttons.add(new TerminalButton(roleType.toString(), roleType.toString(), postButtonHandler));
            }
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.POST_CUSTOM_ROLE_SET, p.botRole()).getData(), postButtonHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        menu.setPages(List.of(page1, page2));
        
        return menu;
    }
}

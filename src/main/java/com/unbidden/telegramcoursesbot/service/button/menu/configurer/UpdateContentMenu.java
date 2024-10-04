package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.handler.UpdateContentButtonHandler;
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
public class UpdateContentMenu implements MenuConfigurer {
    private final UpdateContentButtonHandler updateContentHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsFunction(u -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser("button_update_content", u)
                    .getData(), "updC", updateContentHandler)));
        menu.setName("m_cntUpd");
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(false);
        menu.setUpdateAfterTerminalButtonRequired(false);
        menu.setAttachedToMessage(true);
        menuService.save(menu);
    }
}

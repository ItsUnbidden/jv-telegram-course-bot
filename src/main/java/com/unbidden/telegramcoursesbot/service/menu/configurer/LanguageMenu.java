package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.handler.SelectLanguageButtonHandler;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LanguageMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_sl";

    private static final String DEFAULT_LANGUAGE_CODE = "dlc";

    private static final String MENU_LANGUAGE_PAGE_0 = "menu_language_page_0";

    private static final String BUTTON_DEFAULT_LANGUAGE_CODE = "button_default_language_code";

    private static final String SERVICE_LANGUAGE_CODE = "service_language_code_%s";

    private final SelectLanguageButtonHandler selectLanguageButtonHandler;

    private final MenuService menuService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setLocalizationFunction((u, p, b) -> localizationLoader
                .getLocalizationForUser(MENU_LANGUAGE_PAGE_0, u));
        page.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            
            buttons.addAll(Arrays.stream(textUtil.getLanguagePriority())
                    .map(c -> new TerminalButton(localizationLoader.getLocalizationForUser(
                        SERVICE_LANGUAGE_CODE.formatted(c), u).getData(),
                        c, selectLanguageButtonHandler)).toList());
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_DEFAULT_LANGUAGE_CODE, u).getData(), DEFAULT_LANGUAGE_CODE,
                    selectLanguageButtonHandler));
            return buttons;
        });
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menuService.save(menu);
    }
}

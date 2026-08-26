package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.SelectLanguageButtonHandler;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LanguageMenu implements MenuConfigurer {
    private final SelectLanguageButtonHandler selectLanguageButtonHandler;

    private final TextUtil textUtil;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.LANGUAGE);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(1);
        page.setLocalizationFunction(p -> loader.localize(Localizations.Menu.LANGUAGE_PAGE_0, p.user()));
        page.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            
            buttons.addAll(textUtil.getLanguagePriority().stream()
                    .map(c -> new TerminalButton(loader.getLanguageName(c), c, selectLanguageButtonHandler))
                    .toList());
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.DEFAULT_LANGUAGE_CODE, p.user()).getData(), selectLanguageButtonHandler));

            return buttons;
        });

        menu.setPages(List.of(page));

        return menu;
    }
}

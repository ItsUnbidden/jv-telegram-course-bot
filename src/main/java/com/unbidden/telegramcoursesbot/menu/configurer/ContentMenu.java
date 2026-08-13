package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.GetContentButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.GetMappingButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.UploadContentButtonHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentMenu implements MenuConfigurer { 
    private final GetContentButtonHandler getContentHandler;
    private final UploadContentButtonHandler uploadContentHandler;
    private final GetMappingButtonHandler getMappingHandler;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.CONTENT);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(2);
        page.setLocalizationFunction(p -> loader.localize(Localizations.Menu.CONTENT_ACTIONS_PAGE_0, p.user()));
        page.setButtonsFunction(p -> List.of(
                new TerminalButton(loader.localize(Localizations.Button.UPLOAD_CONTENT, p.user()).getData(), uploadContentHandler),
                new TerminalButton(loader.localize(Localizations.Button.GET_CONTENT, p.user()).getData(), getContentHandler),
                new TerminalButton(loader.localize(Localizations.Button.GET_MAPPING, p.user()).getData(), getMappingHandler)
        ));
        
        menu.setPages(List.of(page));

        return menu;
    }
}

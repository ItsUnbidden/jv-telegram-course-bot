package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.GetContentButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.GetMappingButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.UploadContentButtonHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentMenu implements MenuConfigurer { 
    private static final String UPLOAD_CONTENT = "upC";
    private static final String GET_CONTENT = "gC";
    private static final String GET_MAPPING = "gm";

    private final GetContentButtonHandler getContentHandler;
    private final UploadContentButtonHandler uploadContentHandler;
    private final GetMappingButtonHandler getMappingHandler;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.CONTENT);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setButtonsRowSize(2);
        page.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.CONTENT_ACTIONS_PAGE_0, u));
        page.setButtonsFunction((u, p, b) -> List.of(
                new TerminalButton(loader.localize(Localizations.Button.UPLOAD_CONTENT, u).getData(), UPLOAD_CONTENT, uploadContentHandler),
                new TerminalButton(loader.localize(Localizations.Button.GET_CONTENT, u).getData(), GET_CONTENT, getContentHandler),
                new TerminalButton(loader.localize(Localizations.Button.GET_MAPPING, u).getData(), GET_MAPPING, getMappingHandler)
        ));
        
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);

        return menu;
    }
}

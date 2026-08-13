package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.AddMappingLocalizationButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveMappingLocalizationButtonHandler;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MappingSettingsMenu implements MenuConfigurer {
    private static final String MAPPING_ID_PARAM = "mappingId";

    private final AddMappingLocalizationButtonHandler addMappingLocalizationHandler;
    private final RemoveMappingLocalizationButtonHandler removeMappingLocalizationHandler;

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.MAPPING_SETTINGS);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(2);
        page.setLocalizationFunction(p -> {
            final ContentMapping mapping = entityUtil.getMappingById(p.user(), p.bot(),
                Long.parseLong(p.params().get(MAPPING_ID_PARAM)));

            return loader.localize(Localizations.Menu.MAPPING_SETTINGS_PAGE_0, p.user(),
                new Localizations.Menu.MappingSettingsPage0Params(mapping.getId(), mapping.getPosition(),
                getContentString(mapping)));
        });
        page.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.ADD_MAPPING_LOCALIZATION, p.user()).getData(), addMappingLocalizationHandler),
            new TerminalButton(loader.localize(Localizations.Button.REMOVE_MAPPING_LOCALIZATION, p.user()).getData(), removeMappingLocalizationHandler)
        ));

        menu.setPages(List.of(page));

        return menu;
    }

    private String getContentString(ContentMapping mapping) {
        final StringBuilder builder = new StringBuilder();
        for (LocalizedContent content : mapping.getContent()) {
            builder.append("[").append(content.getId()).append("; ")
                    .append(content.getLanguageCode()).append("], ");
        }
        builder.delete(builder.length() - 2, builder.length());
        return builder.toString();
    }
}

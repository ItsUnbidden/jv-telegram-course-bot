package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.AddMappingLocalizationButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.ContentMappingTextToggleButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.RemoveMappingLocalizationButtonHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MappingSettingsMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_mpgOpt";
    
    private static final String PARAM_CONTENT = "${content}";
    private static final String PARAM_POSITION = "${position}";
    private static final String PARAM_MAPPING_ID = "${mappingId}";
    
    private static final String ADD_MAPPING_LOCALIZATION = "aml";
    private static final String REMOVE_MAPPING_LOCALIZATION = "rml";
    private static final String MAPPING_TEXT_TOGGLE = "mtt";
    
    private static final String BUTTON_REMOVE_MAPPING_LOCALIZATION = "button_remove_mapping_localization";
    private static final String BUTTON_ADD_MAPPING_LOCALIZATION = "button_add_mapping_localization";
    private static final String BUTTON_MAPPING_TEXT_TOGGLE = "button_mapping_text_toggle";

    private static final String MENU_MAPPING_SETTINGS_PAGE_0 = "menu_mapping_settings_page_0";

    private final AddMappingLocalizationButtonHandler addMappingLocalizationHandler;
    private final RemoveMappingLocalizationButtonHandler removeMappingLocalizationHandler;
    private final ContentMappingTextToggleButtonHandler contentMappingTextToggleHandler;

    private final MenuService menuService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(2);
        page.setLocalizationFunction((u, p, b) -> {
            final ContentMapping mapping = contentService.getMappingById(
                Long.parseLong(p.get(0)), u);
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_MAPPING_ID, mapping.getId());
            parameterMap.put(PARAM_POSITION, mapping.getPosition());
            parameterMap.put(PARAM_CONTENT, getContentString(mapping));
            return localizationLoader.getLocalizationForUser(
                MENU_MAPPING_SETTINGS_PAGE_0, u, parameterMap);
        });
        page.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(localizationLoader
                .getLocalizationForUser(BUTTON_ADD_MAPPING_LOCALIZATION, u).getData(),
                    ADD_MAPPING_LOCALIZATION, addMappingLocalizationHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_REMOVE_MAPPING_LOCALIZATION, u)
                    .getData(), REMOVE_MAPPING_LOCALIZATION, removeMappingLocalizationHandler),
                new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_MAPPING_TEXT_TOGGLE, u).getData(), MAPPING_TEXT_TOGGLE,
                    contentMappingTextToggleHandler)));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menuService.save(menu);
    }

    private String getContentString(ContentMapping mapping) {
        final StringBuilder builder = new StringBuilder();
        for (LocalizedContent content : mapping.getContent()) {
            builder.append("[").append(content.getId()).append(";")
                    .append(content.getLanguageCode()).append("], ");
        }
        builder.delete(builder.length() - 2, builder.length());
        return builder.toString();
    }
}

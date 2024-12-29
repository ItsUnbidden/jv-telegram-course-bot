package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.handler.ImageFileUploadButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.LocalizationFileUploadButtonHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_fls";
    
    private static final String BUTTON_UPLOAD_IMAGE_FILE = "button_upload_image_file";
    private static final String BUTTON_UPLOAD_LOCALIZATION_FILE = "button_upload_localization_file";
    
    private static final String UPLOAD_IMAGE_FILE = "uif";
    private static final String UPLOAD_LOCALIZATION_FILE = "ulf";

    private static final String MENU_FILES_PAGE_0 = "menu_files_page_0";

    private final LocalizationFileUploadButtonHandler localizationFileUploadHandler;
    private final ImageFileUploadButtonHandler imageFileUploadHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setLocalizationFunction((u, p, b) -> localizationLoader
                .getLocalizationForUser(MENU_FILES_PAGE_0, u));
        page.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_UPLOAD_LOCALIZATION_FILE, u).getData(), UPLOAD_LOCALIZATION_FILE,
                    localizationFileUploadHandler));

            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_UPLOAD_IMAGE_FILE, u).getData(), UPLOAD_IMAGE_FILE,
                    imageFileUploadHandler));
            return buttons;
        });
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menuService.save(menu);
    }
}

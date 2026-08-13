package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.DeleteInvoiceImageButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.ImageFileUploadButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.LocalizationFileUploadButtonHandler;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileMenu implements MenuConfigurer {
    private final LocalizationFileUploadButtonHandler localizationFileUploadHandler;
    private final ImageFileUploadButtonHandler imageFileUploadHandler;
    private final DeleteInvoiceImageButtonHandler deleteInvoiceImageHandler;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.FILE);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(1);
        page.setLocalizationFunction(p -> loader.localize(Localizations.Menu.FILES_PAGE_0, p.user()));
        page.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.UPLOAD_LOCALIZATION_FILE, p.user()).getData(), localizationFileUploadHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.UPLOAD_IMAGE_FILE, p.user()).getData(), imageFileUploadHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.DELETE_INVOICE_IMAGE, p.user()).getData(), deleteInvoiceImageHandler));
            
            return buttons;
        });

        menu.setPages(List.of(page));
        
        return menu;
    }
}

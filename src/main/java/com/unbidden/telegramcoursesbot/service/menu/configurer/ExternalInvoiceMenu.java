package com.unbidden.telegramcoursesbot.service.menu.configurer;

import java.util.List;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.LinkButton;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExternalInvoiceMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_extInv";

    private static final String BUTTON_EXTERNAL_INVOICE_MORE_INFO = "button_external_invoice_more_info";

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page firstPage = new Page();

        firstPage.setPageIndex(0);
        firstPage.setButtonsRowSize(1);
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction((u, p, b) -> List.of(new LinkButton(localizationLoader
            .localize(BUTTON_EXTERNAL_INVOICE_MORE_INFO, u).getData(), p.get(0))));

        menu.setName(MENU_NAME);
        menu.setPages(List.of(firstPage));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(false);
        menu.setUpdateAfterTerminalButtonRequired(false);
        menu.setAttachedToMessage(true);
        menuService.save(menu);
    }
}

package com.unbidden.telegramcoursesbot.menu.configurer;

import java.util.List;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.LinkButton;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExternalInvoiceMenu implements MenuConfigurer {
    private static final String COURSE_ID_PARAM = "courseId";

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.EXTERNAL_INVOICE);

        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setColumns(1);
        firstPage.setButtonsFunction(p -> List.of(new LinkButton(loader.localize(Localizations.Button.EXTERNAL_INVOICE_MORE_INFO, p.user()).getData(),
                entityUtil.getCourseById(p.user(), p.bot(), Long.parseLong(p.params().get(COURSE_ID_PARAM))).getExternalInvoice().getExternalStorePageUrl())));

        menu.setPages(List.of(firstPage));

        return menu;
    }
}

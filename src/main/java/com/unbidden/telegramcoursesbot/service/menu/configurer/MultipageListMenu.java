package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.MultipageListMeta;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MultipageListMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_mpl";
    
    private static final String MULTIPAGE_LIST_BACK = "mplb";
    private static final String MULTIPAGE_LIST_NEXT = "mpln";

    private static final String BUTTON_MULTIPAGE_LIST_BACK = "button_multipage_list_back";
    private static final String BUTTON_MULTIPAGE_LIST_NEXT = "button_multipage_list_next";
    
    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;
    
    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(2);
        page.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            final MultipageListMeta meta = menuService.getMultipageListMeta(
                    Integer.parseInt(p.getFirst()), u);
            int factor = 0;

            if (meta.getPage() > 0) {
                buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_MULTIPAGE_LIST_BACK, u).getData(), MULTIPAGE_LIST_BACK,
                    (u1, p1, b1) -> {
                        meta.setPage(meta.getPage() - 1);
                        menuService.processMultipageListRequest(meta);
                        if (meta.getPage() == 0) {
                            meta.setControlMenuUpdateRequired(true);
                        }
                }));
                factor++;
            }
            if (meta.getPage() < meta.getAmountOfPages() - 1) {
                buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_MULTIPAGE_LIST_NEXT, u).getData(), MULTIPAGE_LIST_NEXT,
                    (u1, p1, b1) -> {
                        meta.setPage(meta.getPage() + 1);
                        menuService.processMultipageListRequest(meta);
                        if (meta.getPage() == meta.getAmountOfPages() - 1) {
                            meta.setControlMenuUpdateRequired(true);
                        }
                }));
                factor++;
            }
            if (factor == 1) {
                meta.setControlMenuUpdateRequired(true);
            }
            return buttons;
        });
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(true);
        menu.setAttachedToMessage(true);
        menuService.save(menu);
    }
}

package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MultipageListMeta;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.TestButtonHandler;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MultipageListMenu implements MenuConfigurer {
    private final TestButtonHandler testButtonHandler;
    
    // private static final String MULTIPAGE_LIST_BACK = "mplb";
    // private static final String MULTIPAGE_LIST_NEXT = "mpln";

    // private static final String BUTTON_MULTIPAGE_LIST_BACK = "button_multipage_list_back";
    // private static final String BUTTON_MULTIPAGE_LIST_NEXT = "button_multipage_list_next";
    
    private final MenuOrchestrationService menuService;

    private final LocalizationLoader localizationLoader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.MULTIPAGE_LIST);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(2);
        page.setButtonsFunction(p -> {
            // final List<Button> buttons = new ArrayList<>();
            // final MultipageListMeta meta = menuService.getMultipageListMeta(
            //         Integer.parseInt(p.getFirst()), p.bot());
            // int factor = 0;

            // if (meta.getNextPage() > 0) {
            //     buttons.add(new TerminalButton(localizationLoader.localize(
            //         BUTTON_MULTIPAGE_LIST_BACK, u).getData(), MULTIPAGE_LIST_BACK,
            //         (u1, p1, b1) -> {
            //             meta.setNextPage(meta.getNextPage() - 1);
            //             menuService.processMultipageListRequest(meta);
            //             if (meta.getNextPage() == 0) {
            //                 meta.setControlMenuUpdateRequired(true);
            //             }
            //     }));
            //     factor++;
            // }
            // if (meta.getNextPage() < meta.getNumberOfPages() - 1) {
            //     buttons.add(new TerminalButton(localizationLoader.localize(
            //         BUTTON_MULTIPAGE_LIST_NEXT, u).getData(), MULTIPAGE_LIST_NEXT,
            //         (u1, p1, b1) -> {
            //             meta.setNextPage(meta.getNextPage() + 1);
            //             menuService.processMultipageListRequest(meta);
            //             if (meta.getNextPage() == meta.getNumberOfPages() - 1) {
            //                 meta.setControlMenuUpdateRequired(true);
            //             }
            //     }));
            //     factor++;
            // }
            // if (factor == 1) {
            //     meta.setControlMenuUpdateRequired(true);
            // }
            // return buttons;

            return List.of(new TerminalButton("Dummy", testButtonHandler));
        });

        menu.setPages(List.of(page));

        return menu;
    }
}

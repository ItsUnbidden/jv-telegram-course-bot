package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoursesMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_crs";
    private static final String MY_COURSES_MENU_NAME = "m_myCrs";

    private final LocalizationLoader loader;

    private final CourseService courseService;

    private final MenuService menuService;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.COURSES);

        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setButtonsRowSize(2);
        firstPage.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.COURSES_PAGE_0, u));
        firstPage.setButtonsFunction((u, p, b) -> List.of(
            new TransitoryButton(loader.localize(Localizations.Button.AVAILABLE_COURSES, u).getData(), AVAILABLE_COURSES, 1),
            new TerminalButton(loader.localize(Localizations.Button.MY_COURSES, u).getData(), MY_COURSES, (b1, u1, pa) -> menuService.initiateMenu(MY_COURSES_MENU_NAME, u1, b1))
        ));

        final Page secondPage = new Page(menu);

        secondPage.setPageIndex(1);
        secondPage.setPreviousPage(0);
        secondPage.setButtonsRowSize(2);
        secondPage.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.COURSES_PAGE_1, u));
        secondPage.setButtonsFunction((u, p, b) -> {
            final List<String> ownedCoursesNames = courseService.getAllOwnedByUser(u, b).stream()
                    .map(c -> c.getName()).toList();
            final List<String> allCoursesNames = courseService.getByBot(b).stream()
                    .filter(c -> !c.isUnderMaintenance()).map(c -> c.getName()).toList();
            final List<Button> buttons = new ArrayList<>();
            buttons.addAll(allCoursesNames.stream().filter(cn -> !ownedCoursesNames.contains(cn))
                    .map(cn -> (Button)new TerminalButton(loader
                    .localize(COURSE_NAME.formatted(cn), u).getData(), cn,
                    (b1, p1, u1) -> courseService.initMessage(u, b1, cn))).toList());
            buttons.add(new BackwardButton(loader.localize(
                    BUTTON_BACK, u).getData()));
            return buttons;
        });
        
        menu.setPages(List.of(firstPage, secondPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(false);
        menu.setUpdateAfterTerminalButtonRequired(true);

        return menu;
    }
}

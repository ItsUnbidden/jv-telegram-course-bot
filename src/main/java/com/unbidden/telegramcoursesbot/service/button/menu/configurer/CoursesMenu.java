package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoursesMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_crs";
    private static final String MY_COURSES_MENU_NAME = "m_myCrs";

    private static final String MY_COURSES = "mC";
    private static final String AVAILABLE_COURSES = "aC";

    private static final String BUTTON_MY_COURSES = "button_my_courses";
    private static final String BUTTON_AVAILABLE_COURSES = "button_available_courses";

    private static final String COURSE_NAME = "course_%s_name";

    private static final String MENU_COURSES_PAGE_1 = "menu_courses_page_1";
    private static final String MENU_COURSES_PAGE_0 = "menu_courses_page_0";

    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final MenuService menuService;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setButtonsRowSize(2);
        firstPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_COURSES_PAGE_0, u));
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction((u, p) -> List.of(new TransitoryButton(localizationLoader
                .getLocalizationForUser(BUTTON_AVAILABLE_COURSES, u).getData(),
                AVAILABLE_COURSES, 1), new TerminalButton(localizationLoader
                .getLocalizationForUser(BUTTON_MY_COURSES, u).getData(), MY_COURSES, (u1, pa) ->
                menuService.initiateMenu(MY_COURSES_MENU_NAME, u1))));
        final Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setButtonsRowSize(2);
        secondPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_COURSES_PAGE_1, u));
        secondPage.setMenu(menu);
        secondPage.setButtonsFunction((u, p) -> {
            final List<String> ownedCoursesNames = courseService.getAllOwnedByUser(u).stream()
                    .map(c -> c.getName()).toList();
            final List<String> allCoursesNames = courseService.getAll().stream()
                    .map(c -> c.getName()).toList();
            return allCoursesNames.stream().filter(cn -> !ownedCoursesNames.contains(cn))
                    .map(cn -> (Button)new TerminalButton(localizationLoader
                    .getLocalizationForUser(COURSE_NAME.formatted(cn), u).getData(), cn,
                    (p1, u1) -> courseService.initMessage(u, cn))).toList();
        });
        
        menu.setName(MENU_NAME);
        menu.setPages(List.of(firstPage, secondPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(false);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

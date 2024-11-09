package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailableCoursesMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_aCrs";

    private static final String COURSE_NAME = "course_%s_name";

    private static final String MENU_AVAILABLE_COURSES_PAGE_0 = "menu_available_courses_page_0";

    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final MenuService menuService;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_AVAILABLE_COURSES_PAGE_0, u));
        firstPage.setButtonsRowSize(2);
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction((u, p) -> {
            final List<String> ownedCoursesNames = courseService.getAllOwnedByUser(u).stream()
                    .map(c -> c.getName()).toList();
            final List<String> allCoursesNames = courseService.getAll().stream()
                    .map(c -> c.getName()).toList();
            return allCoursesNames.stream().filter(cn -> !ownedCoursesNames.contains(cn))
                    .map(cn -> (Button)new TerminalButton(localizationLoader
                    .getLocalizationForUser(COURSE_NAME.formatted(cn), u).getData(), cn,
                    (u1, pa) -> courseService.initMessage(u, cn))).toList();
        });
        menu.setName(MENU_NAME);
        menu.setPages(List.of(firstPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(false);
        menuService.save(menu);
    }
}

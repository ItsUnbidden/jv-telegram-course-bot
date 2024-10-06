package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailableCoursesMenu implements MenuConfigurer {
    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final MenuService menuService;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            "menu_available_courses_page_0", u));
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction(u -> {
            final List<String> ownedCoursesNames = courseService.getAllOwnedByUser(u).stream()
                    .map(c -> c.getName()).toList();
            final List<String> allCoursesNames = courseService.getAll().stream()
                    .map(c -> c.getName()).toList();
            return allCoursesNames.stream().filter(cn -> !ownedCoursesNames.contains(cn))
                    .map(cn -> (Button)new TerminalButton(localizationLoader
                    .getLocalizationForUser("course_" + cn + "_name", u).getData(), cn,
                    (p1, u1) -> courseService.initMessage(u, cn))).toList();
        });
        menu.setName("m_aCrs");
        menu.setPages(List.of(firstPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setUpdateAfterTerminalButtonRequired(false);
        menu.setAttachedToMessage(false);
        menuService.save(menu);
    }
}

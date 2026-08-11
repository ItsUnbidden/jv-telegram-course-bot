package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.dto.CourseResponseDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.InitiateCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailableCoursesMenu implements MenuConfigurer {
    private final InitiateCourseButtonHandler initiateCourseButtonHandler;

    private final CourseOrchestrationService courseService;
    
    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.AVAILABLE_COURSES);

        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setLocalizationFunction((u, p, b) -> loader.localize(
            Localizations.Menu.AVAILABLE_COURSES_PAGE_0, u));
        firstPage.setButtonsRowSize(2);
        firstPage.setButtonsFunction((u, p, b) -> {
            final List<CourseResponseDto> availableCourses = courseService.getAllAvailableByUser(u, b);
            
            return availableCourses.stream()
                    .map(c -> (Button)new TerminalButton(c.getLocalizedTitle(), c.getId().toString(), initiateCourseButtonHandler))
                    .toList();
        });

        menu.setPages(List.of(firstPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(false);

        return menu;
    }
}

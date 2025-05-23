package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseNextStageMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_crsNxtStg";

    private static final String NEXT_STAGE = "ns";

    private static final String BUTTON_COURSE_NEXT_STAGE = "button_course_next_stage";

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    private final MenuService menuService;
    
    @Override
    public void configure() {
        final Menu courseNextStageMenu = new Menu();
        final Page page = new Page();
        page.setMenu(courseNextStageMenu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_COURSE_NEXT_STAGE, u)
                .getData(), NEXT_STAGE, (u1, pa) -> {
                    final String[] courseNameAndCurrentLesson = pa[0].split(
                            CourseService.COURSE_NAME_LESSON_INDEX_DIVIDER);
                    final CourseProgress courseProgress = courseService
                            .getCurrentCourseProgressForUser(u1.getId(),
                            courseNameAndCurrentLesson[0]);
                    if (courseProgress.getStage().equals(Integer.parseInt(
                            courseNameAndCurrentLesson[1]))) {
                        menuService.terminateMenuGroup(u1, CourseService
                                .COURSE_NEXT_STAGE_MENU_TERMINATION.formatted(
                                courseProgress.getId()));
                        courseService.next(u, courseNameAndCurrentLesson[0]);
                    }
                })));
        final Page terminalPage = new Page();
        terminalPage.setMenu(courseNextStageMenu);
        terminalPage.setPageIndex(1);
        courseNextStageMenu.setName(MENU_NAME);
        courseNextStageMenu.setPages(List.of(page, terminalPage));
        courseNextStageMenu.setInitialParameterPresent(true);
        courseNextStageMenu.setAttachedToMessage(true);
        menuService.save(courseNextStageMenu);
    }
}

package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseNextStageMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_crsNxtStg";

    private static final String NEXT_STAGE = "ns";

    private static final String BUTTON_COURSE_NEXT_STAGE =
            "button_course_%s_lesson_%s_next_stage";

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
        page.setButtonsFunction((u, p, b) ->  {
            final String[] courseNameAndCurrentLesson = p.get(0).split(
                    CourseService.COURSE_NAME_LESSON_INDEX_DIVIDER);
            final CourseProgress courseProgress = courseService
                    .getCurrentCourseProgressForUser(u.getId(),
                    courseNameAndCurrentLesson[0]);
            return List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_COURSE_NEXT_STAGE.formatted(
                    courseProgress.getCourse().getName(), courseProgress.getStage()), u)
                .getData(), NEXT_STAGE, (b1, u1, pa) -> {
                    courseService.checkCourseIsNotUnderMaintenance(courseProgress.getCourse(), u);
                    if (courseProgress.getStage().equals(Integer.parseInt(
                            courseNameAndCurrentLesson[1]))) {
                        menuService.terminateMenuGroup(u1, b1, CourseService
                                .COURSE_NEXT_STAGE_MENU_TERMINATION.formatted(
                                courseProgress.getId()));
                        courseService.next(u, courseNameAndCurrentLesson[0]);
                    }
            }));
        });
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

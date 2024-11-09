package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveBasicReviewMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_lbR";

    private static final String PARAM_COURSE_NAME = "${courseName}";
    
    private static final String MENU_LEAVE_BASIC_REVIEW_TERMINAL_PAGE =
            "menu_leave_basic_review_terminal_page";
    private static final String MENU_LEAVE_BASIC_REVIEW_PAGE_1 = "menu_leave_basic_review_page_1";
    private static final String MENU_LEAVE_BASIC_REVIEW_PAGE_0 = "menu_leave_basic_review_page_0";

    private static final List<Button> COURSE_GRADE_BUTTONS = new ArrayList<>();
    private static final List<Button> PLATFORM_GRADE_BUTTONS = new ArrayList<>();
    private static final int GRADE_OPTIONS = 10;

    private final MenuService menuService;

    private final ReviewService reviewService;

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    @PostConstruct
    public void init() {
        for (int i = 1; i <= GRADE_OPTIONS; i++) {
            final String iSrt = String.valueOf(i);
            COURSE_GRADE_BUTTONS.add(new TransitoryButton(iSrt, iSrt, 1));
            PLATFORM_GRADE_BUTTONS.add(new TerminalButton(iSrt, iSrt, (u, pa) ->
                    reviewService.commitBasicReview(u, courseService.getCourseById(
                    Long.parseLong(pa[0]), u), Integer.parseInt(pa[1]),
                    Integer.parseInt(pa[2]))));
        }
    }

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setButtonsRowSize(5);
        firstPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_LEAVE_BASIC_REVIEW_PAGE_0, u));
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction((u, p) -> COURSE_GRADE_BUTTONS);

        final Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setButtonsRowSize(5);
        secondPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_LEAVE_BASIC_REVIEW_PAGE_1, u));
        secondPage.setMenu(menu);
        secondPage.setButtonsFunction((u, p) -> PLATFORM_GRADE_BUTTONS);

        final Page terminalPage = new Page();
        terminalPage.setPageIndex(2);
        terminalPage.setLocalizationFunction((u, p) -> {
            final Course course = courseService.getCourseById(Long.parseLong(p.get(0)), u);

            return localizationLoader.getLocalizationForUser(
                    MENU_LEAVE_BASIC_REVIEW_TERMINAL_PAGE, u,
                    PARAM_COURSE_NAME, course.getName());
        });
        terminalPage.setMenu(menu);

        menu.setName(MENU_NAME);
        menu.setPages(List.of(firstPage, secondPage, terminalPage));
        menu.setInitialParameterPresent(true);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(false);
        menuService.save(menu);
    }
}

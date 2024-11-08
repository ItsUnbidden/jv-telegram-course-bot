package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.service.button.handler.SendAdvancedReviewButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.UpdateAdvancedReviewButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MyCoursesMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_myCrs";

    private static final String SEND_ADVANCED_REVIEW = "sar";
    private static final String UPDATE_ADVANCED_REVIEW = "uar";
    private static final String UPDATE_PLATFORM_GRADE = "upg";
    private static final String UPDATE_COURSE_GRADE = "ucg";
    private static final String LEAVE_REVIEW = "lR";
    private static final String REVIEW_BASIC_UPDATE_ADVANCED_LEAVE_OPTIONS = "rbualo";
    private static final String REVIEW_UPDATE_OPTIONS = "ruo";
    private static final String BEGIN_COURSE = "bC";

    private static final String BUTTON_SEND_ADVANCED_REVIEW = "button_send_advanced_review";
    private static final String BUTTON_UPDATE_ADVANCED_REVIEW = "button_update_advanced_review";
    private static final String BUTTON_UPDATE_PLATFORM_GRADE = "button_update_platform_grade";
    private static final String BUTTON_UPDATE_COURSE_GRADE = "button_update_course_grade";
    private static final String BUTTON_LEAVE_REVIEW = "button_leave_review";
    private static final String BUTTON_UPDATE_BASIC_REVIEW_LEAVE_ADVANCED_OPTIONS =
            "button_update_basic_review_and_leave_advanced_options";
    private static final String BUTTON_UPDATE_REVIEW_OPTIONS = "button_update_review_options";
    private static final String BUTTON_BEGIN_COURSE = "button_begin_course";

    private static final String COURSE_NAME = "course_%s_name";

    private static final String MENU_MY_COURSES_PAGE_5 = "menu_my_courses_page_5";
    private static final String MENU_MY_COURSES_PAGE_4 = "menu_my_courses_page_4";
    private static final String MENU_MY_COURSES_PAGE_3 = "menu_my_courses_page_3";
    private static final String MENU_MY_COURSES_PAGE_2 = "menu_my_courses_page_2";
    private static final String MENU_MY_COURSES_PAGE_1 = "menu_my_courses_page_1";
    private static final String MENU_MY_COURSES_PAGE_0 = "menu_my_courses_page_0";

    private static final List<Button> COURSE_GRADE_BUTTONS = new ArrayList<>();
    private static final List<Button> PLATFORM_GRADE_BUTTONS = new ArrayList<>();
    private static final int GRADE_OPTIONS = 10;

    private final UpdateAdvancedReviewButtonHandler updateAdvancedReviewHandler;
    private final SendAdvancedReviewButtonHandler sendAdvancedReviewHandler;

    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final ReviewService reviewService;

    private final MenuService menuService;

    @PostConstruct
    public void init() {
        for (int i = 1; i <= GRADE_OPTIONS; i++) {
            final String iSrt = String.valueOf(i);
            final int currentGrade = i;

            COURSE_GRADE_BUTTONS.add(new TerminalButton(iSrt, iSrt, (u, pa) ->
                    reviewService.updateCourseGrade(reviewService.getReviewByCourseAndUser(u,
                    courseService.getCourseByName(pa[0], u)).getId(), currentGrade)));
            PLATFORM_GRADE_BUTTONS.add(new TerminalButton(iSrt, iSrt, (u, pa) ->
                    reviewService.updatePlatformGrade(reviewService.getReviewByCourseAndUser(u,
                    courseService.getCourseByName(pa[0], u)).getId(), currentGrade)));
        }
    }
    
    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setButtonsRowSize(2);
        firstPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_MY_COURSES_PAGE_0, u));
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction((u, p) -> {
            final List<Course> allOwnedByUser = courseService.getAllOwnedByUser(u);
            final List<Button> buttons = new ArrayList<>();

            for (Course course : allOwnedByUser) {
                final String buttonLocName = COURSE_NAME.formatted(course.getName());

                if (courseService.hasCourseBeenCompleted(u, course)) {
                    buttons.add(new TransitoryButton(localizationLoader
                            .getLocalizationForUser(buttonLocName, u)
                            .getData(),course.getName(), 1));
                } else {
                    buttons.add(new TerminalButton(localizationLoader
                            .getLocalizationForUser(buttonLocName, u)
                            .getData(),course.getName(), (u1, pa) -> courseService.initMessage(u,
                            course.getName())));
                }
            }
            return buttons;
        });
        final Page secondPage = new Page();

        secondPage.setPageIndex(1);
        secondPage.setButtonsRowSize(1);
        secondPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_MY_COURSES_PAGE_1, u));
        secondPage.setMenu(menu);
        secondPage.setButtonsFunction((u, p) -> {
                final List<Button> buttons = new ArrayList<>();
                buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_BEGIN_COURSE, u).getData(), BEGIN_COURSE, (u1, pa) ->
                    courseService.initMessage(u1, pa[0])));

                final Course course = courseService.getCourseByName(p.get(0), u);

                if (reviewService.isAdvancedReviewForCourseAndUserAvailable(u, course)) {
                    buttons.add(new TransitoryButton(localizationLoader.getLocalizationForUser(
                            BUTTON_UPDATE_REVIEW_OPTIONS, u).getData(), REVIEW_UPDATE_OPTIONS, 2));
                    return buttons;
                }
                if (reviewService.isBasicReviewForCourseAndUserAvailable(u, course)) {
                    buttons.add(new TransitoryButton(localizationLoader.getLocalizationForUser(
                            BUTTON_UPDATE_BASIC_REVIEW_LEAVE_ADVANCED_OPTIONS, u).getData(),
                            REVIEW_BASIC_UPDATE_ADVANCED_LEAVE_OPTIONS, 3));
                    return buttons;
                }
                buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                        BUTTON_LEAVE_REVIEW, u).getData(), LEAVE_REVIEW, (p1, u1) ->
                        reviewService.initiateBasicReview(u, course)));
                return buttons;
            });
        final Page thirdPage = new Page();

        thirdPage.setPageIndex(2);
        thirdPage.setButtonsRowSize(2);
        thirdPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_MY_COURSES_PAGE_2, u));
        thirdPage.setMenu(menu);
        thirdPage.setButtonsFunction((u, p) -> List.of(new TransitoryButton(localizationLoader
                .getLocalizationForUser(BUTTON_UPDATE_COURSE_GRADE, u).getData(), UPDATE_COURSE_GRADE, 4),
                new TransitoryButton(localizationLoader.getLocalizationForUser(
                BUTTON_UPDATE_PLATFORM_GRADE, u).getData(), UPDATE_PLATFORM_GRADE, 5),
                new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_UPDATE_ADVANCED_REVIEW, u).getData(), UPDATE_ADVANCED_REVIEW,
                updateAdvancedReviewHandler)));
        final Page fourthPage = new Page();

        fourthPage.setPageIndex(3);
        fourthPage.setButtonsRowSize(2);
        fourthPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_MY_COURSES_PAGE_3, u));
        fourthPage.setMenu(menu);
        fourthPage.setButtonsFunction((u, p) -> List.of(new TransitoryButton(localizationLoader
                .getLocalizationForUser(BUTTON_UPDATE_COURSE_GRADE, u).getData(), UPDATE_COURSE_GRADE, 4),
                new TransitoryButton(localizationLoader.getLocalizationForUser(
                BUTTON_UPDATE_PLATFORM_GRADE, u).getData(), UPDATE_PLATFORM_GRADE, 5),
                new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_SEND_ADVANCED_REVIEW, u).getData(), SEND_ADVANCED_REVIEW, sendAdvancedReviewHandler)));
        final Page fifthPage = new Page();

        fifthPage.setPageIndex(4);
        fifthPage.setButtonsRowSize(5);
        fifthPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_MY_COURSES_PAGE_4, u));
        fifthPage.setMenu(menu);
        fifthPage.setButtonsFunction((u, pa) -> COURSE_GRADE_BUTTONS);
        final Page sixthPage = new Page();

        sixthPage.setPageIndex(5);
        sixthPage.setButtonsRowSize(5);
        sixthPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_MY_COURSES_PAGE_5, u));
        sixthPage.setMenu(menu);
        sixthPage.setButtonsFunction((u, pa) -> PLATFORM_GRADE_BUTTONS);

        menu.setName(MENU_NAME);
        menu.setPages(List.of(firstPage, secondPage, thirdPage, fourthPage,
                fifthPage, sixthPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(false);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.dto.CourseResponseDto;
import com.unbidden.telegramcoursesbot.dto.internal.CourseMenuDto;
import com.unbidden.telegramcoursesbot.exception.RefundImpossibleException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Course;
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
import com.unbidden.telegramcoursesbot.service.menu.handler.InitiateCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.RefundButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.SendAdvancedReviewButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.UpdateAdvancedReviewButtonHandler;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.ReviewOrchestrationService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MyCoursesMenu implements MenuConfigurer {
    private static final String MY_COURSES = "mC";
    private static final String AVAILABLE_COURSES = "aC";
    private static final String SEND_ADVANCED_REVIEW = "sar";
    private static final String UPDATE_ADVANCED_REVIEW = "uar";
    private static final String UPDATE_PLATFORM_GRADE = "upg";
    private static final String UPDATE_COURSE_GRADE = "ucg";
    private static final String LEAVE_REVIEW = "lR";
    private static final String REVIEW_BASIC_UPDATE_ADVANCED_LEAVE_OPTIONS = "rbualo";
    private static final String REVIEW_UPDATE_OPTIONS = "ruo";
    private static final String BEGIN_COURSE = "bC";
    private static final String REFUND = "r";
    private static final String SELECT_COURSE_STAGE = "scs";

    private static final String COURSE_ID_MARKER = "c-";

    private static final List<Button> COURSE_GRADE_BUTTONS = new ArrayList<>();
    private static final List<Button> PLATFORM_GRADE_BUTTONS = new ArrayList<>();
    private static final int GRADE_OPTIONS = 10;

    private final InitiateCourseButtonHandler initiateCourseHandler;
    private final UpdateAdvancedReviewButtonHandler updateAdvancedReviewHandler;
    private final SendAdvancedReviewButtonHandler sendAdvancedReviewHandler;
    private final RefundButtonHandler refundHandler;

    private final LocalizationLoader loader;

    private final CourseOrchestrationService courseService;

    private final ReviewOrchestrationService reviewService;

    private final MenuService menuService;

    @PostConstruct
    public void init() {
        for (int i = 1; i <= GRADE_OPTIONS; i++) {
            final String iSrt = String.valueOf(i);
            final int currentGrade = i;

            COURSE_GRADE_BUTTONS.add(new TerminalButton(iSrt, iSrt, (b, u, pa) ->
                    reviewService.updateCourseGrade(reviewService.getReviewByCourseAndUser(u,
                    courseService.getCourseByName(pa[0], u, b)).getId(), u, b, currentGrade)));
            PLATFORM_GRADE_BUTTONS.add(new TerminalButton(iSrt, iSrt, (b, u, pa) ->
                    reviewService.updatePlatformGrade(reviewService.getReviewByCourseAndUser(u,
                    courseService.getCourseByName(pa[0], u, b)).getId(), u, b, currentGrade)));
        }
    }
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.MY_COURSES);

        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setButtonsRowSize(2);
        firstPage.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_0, u));
        firstPage.setButtonsFunction((u, p, b) -> List.of(
            new TransitoryButton(loader.localize(Localizations.Button.AVAILABLE_COURSES, u).getData(), AVAILABLE_COURSES, 1),
            new TransitoryButton(loader.localize(Localizations.Button.MY_COURSES, u).getData(), MY_COURSES, 2)
        ));

        final Page secondPage = new Page(menu);

        secondPage.setPageIndex(1);
        secondPage.setButtonsRowSize(2);
        secondPage.setPreviousPage(0);
        secondPage.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_1, u));
        secondPage.setButtonsFunction((u, p, b) -> {
            final List<CourseResponseDto> availableCourses = courseService.getAllAvailableByUser(u, b);
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(availableCourses.stream()
                .map(c -> (Button)new TerminalButton(c.getLocalizedTitle(), c.getId().toString(), initiateCourseHandler))
                .toList());

            if (!p.isEmpty() && (p.getFirst().equals(AVAILABLE_COURSES) || p.getFirst().equals(MY_COURSES))) {
                buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, u).getData()));
            }

            return buttons;
        });

        final Page thirdPage = new Page(menu);

        thirdPage.setPageIndex(2);
        thirdPage.setButtonsRowSize(2);
        thirdPage.setPreviousPage(0);
        thirdPage.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_2, u));
        thirdPage.setButtonsFunction((u, p, b) -> {
            final List<CourseMenuDto> dtos = courseService.getCourseMenuDtosForOwnedCourses(u, b);
            final List<Button> buttons = new ArrayList<>();

            for (final var dto : dtos) {
                if (dto.isRefundable() || dto.isCompleted()) {
                    buttons.add(new TransitoryButton(dto.localizedTitle(), COURSE_ID_MARKER + Long.toString(dto.courseId()), 3));
                } else {
                    buttons.add(new TerminalButton(dto.localizedTitle(), Long.toString(dto.courseId()), initiateCourseHandler));
                }
            }
           
            if (!p.isEmpty() && (p.getFirst().equals(AVAILABLE_COURSES) || p.getFirst().equals(MY_COURSES))) {
                buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, u).getData()));
            }

            return buttons;
        });

        final Page fourthPage = new Page(menu);

        fourthPage.setPageIndex(3);
        fourthPage.setPreviousPage(2);
        fourthPage.setButtonsRowSize(1);
        fourthPage.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_3, u));
        fourthPage.setButtonsFunction((u, p, b) -> {
                final List<Button> buttons = new ArrayList<>();
                final Long courseId = findCourseId(p);

                buttons.add(new TerminalButton(loader.localize(Localizations.Button.BEGIN_COURSE, u).getData(), courseId.toString(), initiateCourseHandler));

                final CourseMenuDto dto = courseService.getCourseMenuDtoForCourse(u, b, courseId);

                if (dto.isCompleted()) {
                    buttons.add(new TransitoryButton(loader.localize(Localizations.Button.SELECT_COURSE_STAGE, u).getData(), SELECT_COURSE_STAGE, ));

                    if (dto.isAdvancedReviewPresent()) {
                        buttons.add(new TransitoryButton(loader.localize(Localizations.Button.UPDATE_REVIEW_OPTIONS, u).getData(),
                                REVIEW_UPDATE_OPTIONS, ));
                    } else if (dto.isBasicReviewPresent()) {
                        buttons.add(new TransitoryButton(loader.localize(Localizations.Button.UPDATE_BASIC_REVIEW_LEAVE_ADVANCED_OPTIONS, u).getData(),
                                REVIEW_BASIC_UPDATE_ADVANCED_LEAVE_OPTIONS, ));
                    } else {
                        buttons.add(new TerminalButton(loader.localize(Localizations.Button.LEAVE_REVIEW, u).getData(), LEAVE_REVIEW,
                                (u1, b1, p1) -> reviewService.initiateBasicReview(u1, b1, courseId)));
                    }
                }
                if (dto.isRefundable()) {
                    buttons.add(new TerminalButton(loader.localize(Localizations.Button.REFUND, u).getData(), REFUND, refundHandler));
                }
                
                buttons.add(new BackwardButton(localizationLoader.localize(
                        BUTTON_BACK, u).getData()));
                return buttons;
            });

        // final Page thirdPage = new Page();

        // thirdPage.setPageIndex(2);
        // thirdPage.setPreviousPage(1);
        // thirdPage.setButtonsRowSize(2);
        // thirdPage.setLocalizationFunction((u, p, b) -> localizationLoader.localize(
        //     MENU_MY_COURSES_PAGE_2, u));
        // thirdPage.setMenu(menu);
        // thirdPage.setButtonsFunction((u, p, b) -> List.of(new TransitoryButton(localizationLoader
        //         .localize(BUTTON_UPDATE_COURSE_GRADE, u).getData(),
        //         UPDATE_COURSE_GRADE, 4),
        //         new TransitoryButton(localizationLoader.localize(
        //         BUTTON_UPDATE_PLATFORM_GRADE, u).getData(), UPDATE_PLATFORM_GRADE, 5),
        //         new TerminalButton(localizationLoader.localize(
        //         BUTTON_UPDATE_ADVANCED_REVIEW, u).getData(), UPDATE_ADVANCED_REVIEW,
        //         updateAdvancedReviewHandler), new BackwardButton(localizationLoader
        //         .localize(BUTTON_BACK, u).getData())));
        // final Page fourthPage = new Page();

        // fourthPage.setPageIndex(3);
        // fourthPage.setPreviousPage(1);
        // fourthPage.setButtonsRowSize(2);
        // fourthPage.setLocalizationFunction((u, p, b) -> localizationLoader.localize(
        //     MENU_MY_COURSES_PAGE_3, u));
        // fourthPage.setMenu(menu);
        // fourthPage.setButtonsFunction((u, p, b) -> List.of(new TransitoryButton(localizationLoader
        //         .localize(BUTTON_UPDATE_COURSE_GRADE, u).getData(), UPDATE_COURSE_GRADE, 4),
        //         new TransitoryButton(localizationLoader.localize(
        //         BUTTON_UPDATE_PLATFORM_GRADE, u).getData(), UPDATE_PLATFORM_GRADE, 5),
        //         new TerminalButton(localizationLoader.localize(
        //         BUTTON_SEND_ADVANCED_REVIEW, u).getData(), SEND_ADVANCED_REVIEW,
        //         sendAdvancedReviewHandler), new BackwardButton(localizationLoader
        //         .localize(BUTTON_BACK, u).getData())));
        // final Page fifthPage = new Page();

        // fifthPage.setPageIndex(4);
        // fifthPage.setButtonsRowSize(5);
        // fifthPage.setLocalizationFunction((u, p, b) -> localizationLoader.localize(
        //     MENU_MY_COURSES_PAGE_4, u));
        // fifthPage.setMenu(menu);
        // fifthPage.setButtonsFunction((u, p, b) -> COURSE_GRADE_BUTTONS);
        // final Page sixthPage = new Page();

        // sixthPage.setPageIndex(5);
        // sixthPage.setButtonsRowSize(5);
        // sixthPage.setLocalizationFunction((u, p, b) -> localizationLoader.localize(
        //     MENU_MY_COURSES_PAGE_5, u));
        // sixthPage.setMenu(menu);
        // sixthPage.setButtonsFunction((u, p, b) -> PLATFORM_GRADE_BUTTONS);
        // final Page seventhPage = new Page();

        // seventhPage.setPageIndex(6);
        // seventhPage.setPreviousPage(1);
        // seventhPage.setButtonsRowSize(3);
        // seventhPage.setLocalizationFunction((u, p, b) -> localizationLoader
        //     .localize(MENU_MY_COURSES_PAGE_6, u));
        // seventhPage.setMenu(menu);
        // seventhPage.setButtonsFunction((u, p, b) -> {
        //     final List<Button> buttons = new ArrayList<>();
        //     final Course course = courseService.getCourseByName(p.get(0), u, b);

        //     course.getLessons()
        //             .forEach(l -> buttons.add(new TerminalButton(l.getPosition().toString(),
        //             l.getPosition().toString(), (u1, p1, b1) -> courseService.selectStage(
        //                 u, p.get(0), l.getPosition()))));
        //     return buttons;
        // });

        menu.setPages(List.of(
            firstPage,
            secondPage,
            thirdPage,
            fourthPage,
            fifthPage,
            sixthPage,
            seventhPage
        ));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(false);
        menu.setUpdateAfterTerminalButtonRequired(true);
        
        return menu;
    }

    private Long findCourseId(List<String> params) {
        return Long.parseLong(params.stream()
                .filter(s -> s.startsWith(COURSE_ID_MARKER))
                .findAny()
                .get()
                .substring(COURSE_ID_MARKER.length()));
    }
}

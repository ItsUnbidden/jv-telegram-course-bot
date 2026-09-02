package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.dto.CourseResponseDto;
import com.unbidden.telegramcoursesbot.dto.internal.CourseMenuDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.handler.InitiateCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RefundButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.SelectCourseStageButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.SendAdvancedReviewButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.SendBasicReviewButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.UpdateAdvancedReviewButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.UpdateBasicReviewButtonHandler;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoursesMenu implements MenuConfigurer {
    private static final String IS_ADVANCED_REVIEW_PRESENT_PARAM = "isAdvancedReviewPresent";
    private static final String COURSE_ID_PARAM = "courseId";

    private static final List<Button> COURSE_GRADE_BUTTONS = new ArrayList<>();
    private static final int GRADE_OPTIONS = 10;

    private final InitiateCourseButtonHandler initiateCourseHandler;
    private final UpdateAdvancedReviewButtonHandler updateAdvancedReviewHandler;
    private final SendAdvancedReviewButtonHandler sendAdvancedReviewHandler;
    private final RefundButtonHandler refundHandler;
    private final SendBasicReviewButtonHandler sendBasicReviewHandler;
    private final UpdateBasicReviewButtonHandler updateBasicReviewHandler;
    private final SelectCourseStageButtonHandler selectCourseStageHandler;

    private final LocalizationLoader loader;

    private final CourseOrchestrationService courseService;

    @PostConstruct
    public void init() {
        for (int i = 1; i <= GRADE_OPTIONS; i++) {
            final String iStr = String.valueOf(i);

            COURSE_GRADE_BUTTONS.add(new TerminalButton(iStr, iStr, updateBasicReviewHandler));
        }
    }
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.COURSES);

        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setColumns(2);
        firstPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_0, p.botRole()));
        firstPage.setButtonsFunction(p -> List.of(
            new TransitoryButton(loader.localize(Localizations.Button.AVAILABLE_COURSES, p.botRole()).getData(), 1),
            new TransitoryButton(loader.localize(Localizations.Button.MY_COURSES, p.botRole()).getData(), 2)
        ));

        final Page secondPage = new Page(menu);

        secondPage.setPageIndex(1);
        secondPage.setColumns(2);
        secondPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_1, p.botRole()));
        secondPage.setButtonsFunction(p -> {
            final List<CourseResponseDto> availableCourses = courseService.getAllAvailableByUser(p.botRole());
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(availableCourses.stream()
                .map(c -> (Button)new TerminalButton(c.getLocalizedTitle(), c.getId().toString(), initiateCourseHandler))
                .toList());

            if (p.initialPage() != secondPage.getPageIndex()) {
                buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));
            }

            return buttons;
        });

        final Page thirdPage = new Page(menu);

        thirdPage.setPageIndex(2);
        thirdPage.setColumns(2);
        thirdPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_2, p.botRole()));
        thirdPage.setButtonsFunction(p -> {
            final List<CourseMenuDto> dtos = courseService.getCourseMenuDtosForOwnedCourses(p.botRole());
            final List<Button> buttons = new ArrayList<>();

            for (final var dto : dtos) {
                if (dto.isCompleted() || dto.isRefundable()) {
                    buttons.add(new TransitoryButton(dto.localizedTitle(), COURSE_ID_PARAM, Long.toString(dto.courseId()), 3));
                } else {
                    buttons.add(new TerminalButton(dto.localizedTitle(), Long.toString(dto.courseId()), initiateCourseHandler));
                }
            }
           
            if (p.initialPage() != thirdPage.getPageIndex()) {
                buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));
            }

            return buttons;
        });

        final Page fourthPage = new Page(menu);

        fourthPage.setPageIndex(3);
        fourthPage.setColumns(1);
        fourthPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_3, p.botRole()));
        fourthPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            final Long courseId = Long.parseLong(p.params().get(COURSE_ID_PARAM));

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.BEGIN_COURSE, p.botRole()).getData(), courseId.toString(), initiateCourseHandler));

            final CourseMenuDto dto = courseService.getCourseMenuDtoForCourse(p.botRole(), courseId);

            if (dto.isCompleted()) {
                buttons.add(new TransitoryButton(loader.localize(Localizations.Button.SELECT_COURSE_STAGE, p.botRole()).getData(), 6));

                if (dto.isAdvancedReviewPresent() || dto.isBasicReviewPresent()) {
                    buttons.add(new TransitoryButton(loader.localize(Localizations.Button.UPDATE_REVIEW_OPTIONS, p.botRole()).getData(),
                            IS_ADVANCED_REVIEW_PRESENT_PARAM, String.valueOf(dto.isAdvancedReviewPresent()), 4));
                } else {
                    buttons.add(new TerminalButton(loader.localize(Localizations.Button.LEAVE_REVIEW, p.botRole()).getData(), sendBasicReviewHandler));
                }
            }
            if (dto.isRefundable()) {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.REFUND, p.botRole()).getData(), refundHandler));
            }
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page fifthPage = new Page(menu);

        fifthPage.setPageIndex(4);
        fifthPage.setColumns(2);
        fifthPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_4, p.botRole()));
        fifthPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.UPDATE_COURSE_GRADE, p.botRole()).getData(), 5));
            if (Boolean.parseBoolean(p.params().get(IS_ADVANCED_REVIEW_PRESENT_PARAM))) {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.UPDATE_ADVANCED_REVIEW, p.botRole()).getData(), updateAdvancedReviewHandler));
            } else {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.SEND_ADVANCED_REVIEW, p.botRole()).getData(), sendAdvancedReviewHandler));
            }
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page sixthPage = new Page(menu);

        sixthPage.setPageIndex(5);
        sixthPage.setColumns(5);
        sixthPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_5, p.botRole()));
        sixthPage.setButtonsFunction(p -> COURSE_GRADE_BUTTONS);
        
        final Page seventhPage = new Page(menu);

        seventhPage.setPageIndex(6);
        seventhPage.setColumns(3);
        seventhPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.MY_COURSES_PAGE_6, p.botRole()));
        seventhPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            final CourseResponseDto dto = courseService.getById(p.botRole(), Long.parseLong(p.params().get(COURSE_ID_PARAM)));

            for (int i = 0; i < dto.getLessonIds().size(); ++i) {
                buttons.add(new TerminalButton(String.valueOf(i), String.valueOf(i), selectCourseStageHandler));
            }
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        menu.setPages(List.of(
            firstPage,
            secondPage,
            thirdPage,
            fourthPage,
            fifthPage,
            sixthPage,
            seventhPage
        ));

        menu.setResetAfterTerminal(true);
        
        return menu;
    }
}

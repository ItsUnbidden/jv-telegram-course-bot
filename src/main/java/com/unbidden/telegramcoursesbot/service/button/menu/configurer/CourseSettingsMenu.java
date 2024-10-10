package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.model.Content;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.repository.LessonRepository;
import com.unbidden.telegramcoursesbot.service.button.handler.CoursePriceChangeButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.FeedbackInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.GiveOrTakeAwayCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.HomeworkInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseSettingsMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_crsOpt";

    private static final String HOMEWORK = "hw";
    private static final String FEEDBACK = "fb";
    private static final String GIVE_OR_TAKE_COURSE = "gtC";
    private static final String PRICE_CHANGE = "prCh";

    private static final String BUTTON_COURSE_HOMEWORK_SETTING = "button_course_homework_setting";
    private static final String BUTTON_COURSE_FEEDBACK_SETTING = "button_course_feedback_setting";
    private static final String BUTTON_GIVE_OR_TAKE_COURSE = "button_give_or_take_course";
    private static final String BUTTON_COURSE_PRICE_CHANGE = "button_course_price_change";

    private static final String COURSE_NAME = "course_%s_name";

    private static final String MENU_COURSE_SETTINGS_PAGE_1 = "menu_course_settings_page_1";
    private static final String MENU_COURSE_SETTINGS_PAGE_0 = "menu_course_settings_page_0";
    
    private final CoursePriceChangeButtonHandler priceChangeHandler;
    private final GiveOrTakeAwayCourseButtonHandler giveOrTakeAwayCourseHandler;
    private final FeedbackInclusionButtonHandler feedbackHandler;
    private final HomeworkInclusionButtonHandler homeworkHandler;

    private final LessonRepository lessonRepository;

    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final MenuService menuService;
    
    @Override
    public void configure() {
        final Menu courseSettingsMenu = new Menu();
        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setButtonsRowSize(2);
        firstPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                MENU_COURSE_SETTINGS_PAGE_0, u));
        firstPage.setMenu(courseSettingsMenu);
        firstPage.setButtonsFunction((u, p) -> courseService.getAll().stream()
                .map(c -> (Button)new TransitoryButton(localizationLoader
                    .getLocalizationForUser(COURSE_NAME.formatted(c.getName()), u).getData(),
                    c.getName(), 1))
                .toList());

        final Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setLocalizationFunction((u, p) -> {
            final Course course = courseService.getCourseByName(p.get(0));
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("${id}", course.getId());
            parameterMap.put("${name}", course.getName());
            parameterMap.put("${numberOfLessons}", course.getAmountOfLessons());
            parameterMap.put("${price}", course.getPrice());
            parameterMap.put("${isHomeworkIncluded}", course.isHomeworkIncluded());
            parameterMap.put("${isFeedbackIncluded}", course.isFeedbackIncluded());
            StringBuilder lessonsStrBuilder = new StringBuilder();
            for (Lesson lesson : lessonRepository.findByCourseName(course.getName())) {
                lessonsStrBuilder.append("Id: ").append(lesson.getId()).append('\n')
                        .append("Sequence option: ").append(lesson.getSequenceOption())
                        .append('\n').append("Index: ").append(lesson.getIndex())
                        .append('\n').append("Content Ids: ");
                for (Content content : lesson.getStructure()) {
                    lessonsStrBuilder.append(content.getId()).append(", ");
                }
                lessonsStrBuilder.delete(lessonsStrBuilder.length() - 2,
                        lessonsStrBuilder.length() - 1);
                lessonsStrBuilder.append('\n');
                if (lesson.getHomework() != null) {
                    lessonsStrBuilder.append("Homework: Id: ").append(lesson.getHomework()
                            .getId()).append(", Content Id: ").append(lesson.getHomework()
                            .getContent().getId()).append('\n');
                }
            }
            parameterMap.put("${lessons}", lessonsStrBuilder.toString());

            return localizationLoader.getLocalizationForUser(
                MENU_COURSE_SETTINGS_PAGE_1, u, parameterMap);
        });
        secondPage.setMenu(courseSettingsMenu);
        secondPage.setButtonsFunction((u, p) -> List.of(new TerminalButton(
            localizationLoader.getLocalizationForUser(BUTTON_COURSE_PRICE_CHANGE, u).getData(),
            PRICE_CHANGE, priceChangeHandler), new TerminalButton(
            localizationLoader.getLocalizationForUser(BUTTON_GIVE_OR_TAKE_COURSE, u).getData(),
            GIVE_OR_TAKE_COURSE, giveOrTakeAwayCourseHandler), new TerminalButton(
            localizationLoader.getLocalizationForUser(BUTTON_COURSE_FEEDBACK_SETTING, u)
                .getData(), FEEDBACK, feedbackHandler), new TerminalButton(
            localizationLoader.getLocalizationForUser(BUTTON_COURSE_HOMEWORK_SETTING, u)
                .getData(), HOMEWORK, homeworkHandler)));

        courseSettingsMenu.setName(MENU_NAME);
        courseSettingsMenu.setPages(List.of(firstPage, secondPage));
        courseSettingsMenu.setInitialParameterPresent(false);
        courseSettingsMenu.setOneTimeMenu(false);
        courseSettingsMenu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(courseSettingsMenu);
    }
}

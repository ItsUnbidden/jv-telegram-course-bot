package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.service.button.handler.AddContentToLessonButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.CoursePriceChangeButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.CreateCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.CreateHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.FeedbackInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.GiveOrTakeAwayCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.HomeworkFeedbackToggleButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.HomeworkInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.HomeworkMediaTypesChangeButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.HomeworkRepeatedCompletionToggleButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.RemoveContentFromLessonButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.RemoveCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.UpdateContentPositionButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.UpdateHomeworkContentButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseSettingsMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_crsOpt";
    
    private static final String HOMEWORK_TOGGLE = "hw";
    private static final String FEEDBACK_TOGGLE = "fb";
    private static final String GIVE_OR_TAKE_COURSE = "gtC";
    private static final String PRICE_CHANGE = "prCh";
    private static final String CREATE_COURSE = "cnc";
    private static final String UPDATE_HOMEWORK_CONTENT = "uhc";
    private static final String REMOVE_CONTENT_FROM_LESSON = "rcl";
    private static final String ADD_CONTENT_TO_LESSON = "acl";
    private static final String LESSONS = "l";
    private static final String CREATE_HOMEWORK = "ch";
    private static final String HOMEWORK_REPEATED_COMPLETION = "hrc";
    private static final String HOMEWORK_FEEDBACK = "hf";
    private static final String REMOVE_COURSE = "rc";
    private static final String UPDATE_MEDIA_TYPES = "umt";
    private static final String CHANGE_MAPPING_ORDER = "cmo";

    private static final String PARAM_IS_FEEDBACK_INCLUDED = "${isFeedbackIncluded}";
    private static final String PARAM_IS_HOMEWORK_INCLUDED = "${isHomeworkIncluded}";
    private static final String PARAM_PRICE = "${price}";
    private static final String PARAM_NUMBER_OF_LESSONS = "${numberOfLessons}";
    private static final String PARAM_COURSE_NAME = "${name}";
    private static final String PARAM_COURSE_ID = "${id}";
    private static final String PARAM_MAPPING_IDS = "${mappingIds}";
    private static final String PARAM_SEQUENCE_OPTION = "${sequenceOption}";
    private static final String PARAM_HOMEWORK_ID = "${homeworkId}";
    private static final String PARAM_INDEX = "${index}";
    private static final String PARAM_LESSON_ID = "${lessonId}";
    private static final String PARAM_HOMEWORK_REPEATED_COMPLETION =
            "${homeworkRepeatedCompletion}";
    private static final String PARAM_HOMEWORK_FEEDBACK = "${homeworkFeedback}";
    private static final String PARAM_HOMEWORK_MAPPING = "${homeworkMappingId}";
    private static final String PARAM_HOMEWORK_MEDIA_TYPES = "${homeworkMediaTypes}";

    private static final String BUTTON_COURSE_HOMEWORK_SETTING = "button_course_homework_setting";
    private static final String BUTTON_COURSE_FEEDBACK_SETTING = "button_course_feedback_setting";
    private static final String BUTTON_GIVE_OR_TAKE_COURSE = "button_give_or_take_course";
    private static final String BUTTON_COURSE_PRICE_CHANGE = "button_course_price_change";
    private static final String BUTTON_CREATE_NEW_COURSE = "button_create_new_course";
    private static final String BUTTON_HOMEWORK_SETTINGS = "button_homework_settings";
    private static final String BUTTON_UPDATE_HOMEWORK_CONTENT = "button_update_homework_content";
    private static final String BUTTON_REMOVE_CONTENT_FROM_LESSON =
            "button_remove_content_from_lesson";
    private static final String BUTTON_ADD_CONTENT_TO_LESSON = "button_add_content_to_lesson";
    private static final String BUTTON_COURSE_LESSONS = "button_course_lessons";
    private static final String BUTTON_CREATE_HOMEWORK = "button_create_homework";
    private static final String BUTTON_HOMEWORK_REPEATED_COMPLETION =
            "button_homework_repeated_completion";
    private static final String BUTTON_HOMEWORK_FEEDBACK = "button_homework_feedback";
    private static final String BUTTON_UPDATE_MEDIA_TYPES = "button_update_media_types";
    private static final String BUTTON_REMOVE_COURSE = "button_remove_course";
    private static final String BUTTON_CHANGE_MAPPING_ORDER = "button_change_mapping_order";

    private static final String COURSE_NAME = "course_%s_name";

    private static final String MENU_COURSE_SETTINGS_PAGE_0 = "menu_course_settings_page_0";
    private static final String MENU_COURSE_SETTINGS_PAGE_1 = "menu_course_settings_page_1";
    private static final String MENU_COURSE_SETTINGS_PAGE_2 = "menu_course_settings_page_2";
    private static final String MENU_COURSE_SETTINGS_PAGE_3 = "menu_course_settings_page_3";
    private static final String MENU_COURSE_SETTINGS_PAGE_4 = "menu_course_settings_page_4";
    
    private final CoursePriceChangeButtonHandler priceChangeHandler;
    private final GiveOrTakeAwayCourseButtonHandler giveOrTakeAwayCourseHandler;
    private final FeedbackInclusionButtonHandler feedbackHandler;
    private final HomeworkInclusionButtonHandler homeworkHandler;
    private final CreateCourseButtonHandler createCourseHandler;
    private final AddContentToLessonButtonHandler addContentToLessonHandler;
    private final RemoveContentFromLessonButtonHandler removeContentFromLessonHandler;
    private final UpdateHomeworkContentButtonHandler updateHomeworkContentHandler;
    private final CreateHomeworkButtonHandler createHomeworkHandler;
    private final HomeworkMediaTypesChangeButtonHandler homeworkMediaTypesHandler;
    private final HomeworkFeedbackToggleButtonHandler homeworkFeedbackHandler;
    private final HomeworkRepeatedCompletionToggleButtonHandler homeworkRepeatedCompletionHandler;
    private final RemoveCourseButtonHandler removeCourseHandler;
    private final UpdateContentPositionButtonHandler updateContentPositionHandler;

    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final LessonService lessonService;

    private final HomeworkService homeworkService;

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
        firstPage.setButtonsFunction((u, p) -> {
            final List<Button> buttons = new ArrayList<>();
            buttons.addAll(courseService.getAll().stream()
                .map(c -> (Button)new TransitoryButton(localizationLoader
                    .getLocalizationForUser(COURSE_NAME.formatted(c.getName()), u).getData(),
                    c.getName(), 1))
                .toList());
            buttons.add(new TerminalButton(BUTTON_CREATE_NEW_COURSE, CREATE_COURSE,
                createCourseHandler));
            return buttons;
        });

        final Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setButtonsRowSize(2);
        secondPage.setLocalizationFunction((u, p) -> {
            final Course course = courseService.getCourseByName(p.get(0), u);
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_COURSE_ID, course.getId());
            parameterMap.put(PARAM_COURSE_NAME, course.getName());
            parameterMap.put(PARAM_NUMBER_OF_LESSONS, course.getAmountOfLessons());
            parameterMap.put(PARAM_PRICE, course.getPrice());
            parameterMap.put(PARAM_IS_HOMEWORK_INCLUDED, course.isHomeworkIncluded());
            parameterMap.put(PARAM_IS_FEEDBACK_INCLUDED, course.isFeedbackIncluded());

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
                .getData(), FEEDBACK_TOGGLE, feedbackHandler), new TerminalButton(
            localizationLoader.getLocalizationForUser(BUTTON_COURSE_HOMEWORK_SETTING, u)
                .getData(), HOMEWORK_TOGGLE, homeworkHandler), new TransitoryButton(
            localizationLoader.getLocalizationForUser(BUTTON_COURSE_LESSONS, u)
                .getData(), LESSONS, 2), new TerminalButton(
            localizationLoader.getLocalizationForUser(BUTTON_REMOVE_COURSE, u)
                .getData(), REMOVE_COURSE, removeCourseHandler)));
        
        final Page thirdPage = new Page();
        thirdPage.setPageIndex(2);
        thirdPage.setButtonsRowSize(3);
        thirdPage.setMenu(courseSettingsMenu);
        thirdPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                MENU_COURSE_SETTINGS_PAGE_2, u));
        thirdPage.setButtonsFunction((u, p) -> courseService
                .getCourseByName(p.get(0), u).getLessons().stream()
                .map(l -> (Button)new TransitoryButton(l.getPosition().toString(),
                l.getId().toString(), 3)).toList());
        
        final Page fourthPage = new Page();
        fourthPage.setPageIndex(3);
        fourthPage.setButtonsRowSize(2);
        fourthPage.setMenu(courseSettingsMenu);
        fourthPage.setLocalizationFunction((u, p) -> {
            final Lesson lesson = lessonService.getById(Long.parseLong(p.get(2)), u);
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_LESSON_ID, lesson.getId());
            parameterMap.put(PARAM_INDEX, lesson.getPosition());
            parameterMap.put(PARAM_HOMEWORK_ID, (lesson.getHomework() != null)
                    ? lesson.getHomework().getId() : "Not available");
            parameterMap.put(PARAM_SEQUENCE_OPTION, lesson.getSequenceOption());
            parameterMap.put(PARAM_MAPPING_IDS, lesson.getStructure().stream()
                    .map(c -> c.getId()).toList().toString());
            return localizationLoader.getLocalizationForUser(MENU_COURSE_SETTINGS_PAGE_3,
                    u, parameterMap);            
        });
        fourthPage.setButtonsFunction((u, p) -> {
            final List<Button> buttons = new ArrayList<>();
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_ADD_CONTENT_TO_LESSON, u).getData(), ADD_CONTENT_TO_LESSON,
                addContentToLessonHandler));
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_REMOVE_CONTENT_FROM_LESSON, u).getData(), REMOVE_CONTENT_FROM_LESSON,
                removeContentFromLessonHandler));
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_CHANGE_MAPPING_ORDER, u).getData(), CHANGE_MAPPING_ORDER,
                updateContentPositionHandler));
            final Lesson lesson = lessonService.getById(Long.parseLong(p.get(2)), u);
            final Button homeworkButton;
            if (lesson.getHomework() == null) {
                homeworkButton = new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_CREATE_HOMEWORK, u).getData(), CREATE_HOMEWORK,
                    createHomeworkHandler);
            } else {
                homeworkButton = new TransitoryButton(localizationLoader.getLocalizationForUser(
                    BUTTON_HOMEWORK_SETTINGS, u).getData(), lesson.getHomework()
                    .getId().toString(), 4);
            }
            buttons.add(homeworkButton);
            return buttons;
        });

        final Page fifthPage = new Page();
        fifthPage.setPageIndex(4);
        fifthPage.setButtonsRowSize(2);
        fifthPage.setMenu(courseSettingsMenu);
        fifthPage.setLocalizationFunction((u, p) -> {
            final Homework homework = homeworkService.getHomework(Long.parseLong(p.get(3)), u);
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_LESSON_ID, homework.getLesson().getId());
            parameterMap.put(PARAM_HOMEWORK_ID, homework.getId());
            parameterMap.put(PARAM_HOMEWORK_MEDIA_TYPES,
                (homework.getAllowedMediaTypes() != null
                && !homework.getAllowedMediaTypes().isBlank())
                ? homework.getAllowedMediaTypes() : "Not available");
            parameterMap.put(PARAM_HOMEWORK_MAPPING, homeworkService.getHomework(
                homework.getId(), u).getMapping().getId());
            parameterMap.put(PARAM_HOMEWORK_FEEDBACK, homework.isFeedbackRequired());
            parameterMap.put(PARAM_HOMEWORK_REPEATED_COMPLETION,
                homework.isRepeatedCompletionAvailable());

            return localizationLoader.getLocalizationForUser(
                MENU_COURSE_SETTINGS_PAGE_4, u, parameterMap);
        });
        fifthPage.setButtonsFunction((u, p) -> List.of(new TerminalButton(
            localizationLoader.getLocalizationForUser(BUTTON_UPDATE_HOMEWORK_CONTENT, u)
                .getData(), UPDATE_HOMEWORK_CONTENT, updateHomeworkContentHandler),
            new TerminalButton(localizationLoader.getLocalizationForUser(
            BUTTON_UPDATE_MEDIA_TYPES, u).getData(), UPDATE_MEDIA_TYPES,
                homeworkMediaTypesHandler), new TerminalButton(
            localizationLoader.getLocalizationForUser(BUTTON_HOMEWORK_FEEDBACK, u).getData(),
                HOMEWORK_FEEDBACK, homeworkFeedbackHandler), new TerminalButton(
            localizationLoader.getLocalizationForUser(BUTTON_HOMEWORK_REPEATED_COMPLETION, u)
                .getData(), HOMEWORK_REPEATED_COMPLETION,
                homeworkRepeatedCompletionHandler)));

        courseSettingsMenu.setName(MENU_NAME);
        courseSettingsMenu.setPages(List.of(firstPage, secondPage, thirdPage, fourthPage, fifthPage));
        courseSettingsMenu.setInitialParameterPresent(false);
        courseSettingsMenu.setOneTimeMenu(false);
        courseSettingsMenu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(courseSettingsMenu);
    }
}

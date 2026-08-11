package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.dto.CourseResponseDto;
import com.unbidden.telegramcoursesbot.dto.HomeworkResponseDto;
import com.unbidden.telegramcoursesbot.dto.LessonResponseDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.AddContentToLessonButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.AddLessonToCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.CourseMaintenanceToggleButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.CoursePriceChangeButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.CreateCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.CreateHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.FeedbackInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.GiveOrTakeAwayCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.HomeworkDelaySettingButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.HomeworkFeedbackToggleButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.HomeworkInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.HomeworkMediaTypesChangeButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.HomeworkRepeatedCompletionToggleButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.LessonDelaySettingButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.RemoveContentFromLessonButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.RemoveCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.RemoveLessonFromCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.UpdateContentPositionButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.UpdateCourseRefundStageButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.UpdateHomeworkContentButtonHandler;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.LessonOrchestrationService;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseSettingsMenu implements MenuConfigurer {
    private static final String REMOVE_LESSON = "rl";
    private static final String ADD_LESSON = "al";
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
    private static final String UPDATE_REFUND_STAGE = "urs";
    private static final String SET_HOMEWORK_DELAY = "shd";
    private static final String SET_LESSON_DELAY = "sld";
    private static final String COURSE_MAINTENANCE_TOGGLE = "cmt";
    
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
    private final UpdateCourseRefundStageButtonHandler updateCourseRefundStageHandler;
    private final AddLessonToCourseButtonHandler addLessonToCourseHandler;
    private final RemoveLessonFromCourseButtonHandler removeLessonFromCourseHandler;
    private final LessonDelaySettingButtonHandler lessonDelaySettingHandler;
    private final HomeworkDelaySettingButtonHandler homeworkDelaySettingHandler;
    private final CourseMaintenanceToggleButtonHandler courseMaintenanceToggleHandler;

    private final LocalizationLoader loader;

    private final CourseOrchestrationService courseService;

    private final LessonOrchestrationService lessonService;

    private final HomeworkOrchestrationService homeworkService;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.COURSE_SETTINGS);
        
        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setButtonsRowSize(2);
        firstPage.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_0, u));
        firstPage.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(courseService.getByBot(u, b).stream()
                .map(c -> (Button)new TransitoryButton(c.getLocalizedTitle(), c.getId().toString(), 1))
                .toList());
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.CREATE_NEW_COURSE, u).getData(), CREATE_COURSE, createCourseHandler));

            return buttons;
        });

        final Page secondPage = new Page(menu);

        secondPage.setPageIndex(1);
        secondPage.setPreviousPage(0);
        secondPage.setButtonsRowSize(2);
        secondPage.setLocalizationFunction((u, p, b) -> {
            final CourseResponseDto dto = courseService.getById(u, b, Long.parseLong(p.get(0)));
            final String notAvailable = loader.localize(Localizations.Service.NOT_AVAILABLE, u).getData();

            return loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_1, u, new Localizations.Menu.CourseSettingsPage1(
                dto.getId(),
                dto.getLocalizedTitle(),
                dto.getTitleId(),
                dto.getDescriptionId() != null ? dto.getDescriptionId().toString() : notAvailable,
                dto.getEndId() != null ? dto.getEndId().toString() : notAvailable,
                dto.getPaymentType(),
                dto.getLessonIds().size(),
                dto.getPrice(),
                dto.getRefundStage() >= 0 ? dto.getRefundStage().toString() : notAvailable,
                dto.getExternalStorePageUrl() != null ? dto.getExternalStorePageUrl() : notAvailable,
                dto.getExternalInvoiceMappingId() != null ? dto.getExternalInvoiceMappingId().toString() : notAvailable,
                dto.isHomeworkIncluded(),
                dto.isFeedbackIncluded()
            ));
        });
        secondPage.setButtonsFunction((u, p, b) -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.COURSE_PRICE_CHANGE, u).getData(), PRICE_CHANGE, priceChangeHandler),
            new TerminalButton(loader.localize(Localizations.Button.GIVE_OR_TAKE_COURSE, u).getData(), GIVE_OR_TAKE_COURSE, giveOrTakeAwayCourseHandler),
            new TerminalButton(loader.localize(Localizations.Button.COURSE_FEEDBACK_SETTING, u).getData(), FEEDBACK_TOGGLE, feedbackHandler),
            new TerminalButton(loader.localize(Localizations.Button.COURSE_HOMEWORK_SETTING, u).getData(), HOMEWORK_TOGGLE, homeworkHandler),
            new TerminalButton(loader.localize(Localizations.Button.TOGGLE_COURSE_MAINTENANCE, u).getData(), COURSE_MAINTENANCE_TOGGLE, courseMaintenanceToggleHandler),
            new TerminalButton(loader.localize(Localizations.Button.UPDATE_REFUND_STAGE, u).getData(), UPDATE_REFUND_STAGE, updateCourseRefundStageHandler),
            new TransitoryButton(loader.localize(Localizations.Button.COURSE_LESSONS, u).getData(), LESSONS, 2),
            new TerminalButton(loader.localize(Localizations.Button.REMOVE_COURSE, u).getData(), REMOVE_COURSE, removeCourseHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, u).getData())
        ));

        final Page thirdPage = new Page(menu);

        thirdPage.setPageIndex(2);
        thirdPage.setPreviousPage(1);
        thirdPage.setButtonsRowSize(3);
        thirdPage.setLocalizationFunction((u, p, b) -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_2, u));
        thirdPage.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(lessonService.getCourseLessons(Long.parseLong(p.get(0))).stream()
                .map(l -> (Button)new TransitoryButton(l.getPosition().toString(),
                    l.getId().toString(), 3)).toList());
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.CREATE_LESSON, u).getData(), ADD_LESSON, addLessonToCourseHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, u).getData()));

            return buttons;
        });
        
        final Page fourthPage = new Page(menu);

        fourthPage.setPageIndex(3);
        fourthPage.setPreviousPage(2);
        fourthPage.setButtonsRowSize(2);
        fourthPage.setLocalizationFunction((u, p, b) -> {
            final LessonResponseDto dto = lessonService.getById(u, b, Long.parseLong(p.get(2)));
            final String notAvailable = loader.localize(Localizations.Service.NOT_AVAILABLE, u).getData();

            return loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_3, u, new Localizations.Menu.CourseSettingsPage3(
                dto.getId(),
                dto.getPosition(),
                dto.getHomeworkId() != null ? dto.getHomeworkId().toString() : notAvailable,
                dto.getDelay() > 0 ? dto.getDelay().toString() : notAvailable,
                dto.getNextLessonButtonTitleMappingId() != null ? dto.getNextLessonButtonTitleMappingId().toString() : notAvailable,
                dto.getMappingIds()));            
        });
        fourthPage.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.ADD_CONTENT_TO_LESSON, u).getData(), ADD_CONTENT_TO_LESSON, addContentToLessonHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.REMOVE_CONTENT_FROM_LESSON, u).getData(), REMOVE_CONTENT_FROM_LESSON, removeContentFromLessonHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.CHANGE_MAPPING_ORDER, u).getData(), CHANGE_MAPPING_ORDER, updateContentPositionHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.SET_LESSON_DELAY, u).getData(), SET_LESSON_DELAY, lessonDelaySettingHandler));

            final LessonResponseDto dto = lessonService.getById(u, b, Long.parseLong(p.get(2)));

            final Button homeworkButton;
            if (dto.getHomeworkId() == null) {
                homeworkButton = new TerminalButton(loader.localize(Localizations.Button.CREATE_HOMEWORK, u).getData(), CREATE_HOMEWORK, createHomeworkHandler);
            } else {
                homeworkButton = new TransitoryButton(loader.localize(Localizations.Button.HOMEWORK_SETTINGS, u).getData(), dto.getHomeworkId().toString(), 4);
            }
            buttons.add(homeworkButton);

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.REMOVE_LESSON, u).getData(), REMOVE_LESSON, removeLessonFromCourseHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, u).getData()));

            return buttons;
        });

        final Page fifthPage = new Page(menu);

        fifthPage.setPageIndex(4);
        fifthPage.setPreviousPage(3);
        fifthPage.setButtonsRowSize(2);
        fifthPage.setLocalizationFunction((u, p, b) -> {
            final HomeworkResponseDto dto = homeworkService.getById(u, b, Long.parseLong(p.get(3)));
            final String notAvailable = loader.localize(Localizations.Service.NOT_AVAILABLE, u).getData();

            return loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_4, u, new Localizations.Menu.CourseSettingsPage4(
                dto.getId(),
                dto.getLessonId(),
                dto.getDelay() > 0 ? dto.getDelay().toString() : notAvailable,
                dto.getMappingId(),
                !dto.getAllowedMediaTypes().isEmpty() ? dto.getAllowedMediaTypes().toString() : notAvailable,
                dto.isFeedbackRequired(),
                dto.isRepeatedCompletionAvailable())
            );
        });
        fifthPage.setButtonsFunction((u, p, b) -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.UPDATE_HOMEWORK_CONTENT, u).getData(), UPDATE_HOMEWORK_CONTENT, updateHomeworkContentHandler),
            new TerminalButton(loader.localize(Localizations.Button.UPDATE_MEDIA_TYPES, u).getData(), UPDATE_MEDIA_TYPES, homeworkMediaTypesHandler),
            new TerminalButton(loader.localize(Localizations.Button.SET_HOMEWORK_DELAY, u).getData(), SET_HOMEWORK_DELAY, homeworkDelaySettingHandler),
            new TerminalButton(loader.localize(Localizations.Button.HOMEWORK_FEEDBACK, u).getData(), HOMEWORK_FEEDBACK, homeworkFeedbackHandler),
            new TerminalButton(loader.localize(Localizations.Button.HOMEWORK_REPEATED_COMPLETION, u).getData(), HOMEWORK_REPEATED_COMPLETION, homeworkRepeatedCompletionHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, u).getData())
        ));

        menu.setPages(List.of(firstPage, secondPage, thirdPage, fourthPage, fifthPage));
        
        return menu;
    }
}

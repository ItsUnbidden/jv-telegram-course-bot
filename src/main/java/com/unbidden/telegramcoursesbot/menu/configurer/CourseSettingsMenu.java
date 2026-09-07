package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.dto.CourseResponseDto;
import com.unbidden.telegramcoursesbot.dto.HomeworkResponseDto;
import com.unbidden.telegramcoursesbot.dto.LessonResponseDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.menu.handler.AddContentToLessonButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.AddLessonToCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.AddMappingLocalizationButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CourseMaintenanceToggleButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CoursePriceChangeButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CreateCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CreateEndMappingButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CreateHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.FeedbackInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.GetContentButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.GiveOrTakeAwayCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkDelaySettingButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkFeedbackToggleButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkMediaTypesChangeButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkRepeatedCompletionToggleButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.LessonDelaySettingButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveContentFromLessonButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveEndMappingButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveLessonFromCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveMappingLocalizationButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.UpdateContentPositionButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.UpdateCourseRefundStageButtonHandler;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseInvoice.PaymentType;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.LessonOrchestrationService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseSettingsMenu implements MenuConfigurer {
    private static final String HOMEWORK_ID_PARAM = "homeworkId";
    private static final String LESSON_ID_PARAM = "lessonId";
    private static final String MAPPING_ID_PARAM = "mappingId";
    private static final String CONTENT_ID_PARAM = "contentId";
    private static final String COURSE_ID_PARAM = "courseId";
    private static final String TEXT_ONLY_PARAM = "isTextOnly";

    private final CoursePriceChangeButtonHandler priceChangeHandler;
    private final GiveOrTakeAwayCourseButtonHandler giveOrTakeAwayCourseHandler;
    private final FeedbackInclusionButtonHandler feedbackHandler;
    private final HomeworkInclusionButtonHandler homeworkHandler;
    private final CreateCourseButtonHandler createCourseHandler;
    private final AddContentToLessonButtonHandler addContentToLessonHandler;
    private final RemoveContentFromLessonButtonHandler removeContentFromLessonHandler;
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
    private final AddMappingLocalizationButtonHandler addMappingLocalizationHandler;
    private final RemoveMappingLocalizationButtonHandler removeMappingLocalizationHandler;
    private final GetContentButtonHandler getContentHandler;
    private final CreateEndMappingButtonHandler createEndMappingHandler;
    private final RemoveEndMappingButtonHandler removeEndMappingHandler;

    private final LocalizationLoader loader;

    private final CourseOrchestrationService courseService;

    private final LessonOrchestrationService lessonService;

    private final ContentOrchestrationService contentService;

    private final HomeworkOrchestrationService homeworkService;

    private final EntityUtil entityUtil;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.COURSE_SETTINGS);
        
        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setColumns(2);
        firstPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_0, p.botRole()));
        firstPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(courseService.getByBot(p.botRole()).stream()
                .map(c -> (Button)new TransitoryButton(c.getLocalizedTitle(), COURSE_ID_PARAM, c.getId().toString(), 1))
                .toList());
            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.CREATE_NEW_COURSE, p.botRole()).getData(), 5));

            return buttons;
        });

        final Page secondPage = new Page(menu);

        secondPage.setPageIndex(1);
        secondPage.setColumns(2);
        secondPage.setLocalizationFunction(p -> {
            final CourseResponseDto dto = courseService.getById(p.botRole(), Long.parseLong(p.params().get(COURSE_ID_PARAM)));
            final String notAvailable = loader.localize(Localizations.Service.NOT_AVAILABLE, p.botRole()).getData();
            final String yes = loader.localize(Localizations.Service.YES, p.botRole()).getData();
            final String no = loader.localize(Localizations.Service.NO, p.botRole()).getData();

            return loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_1, p.botRole(), new Localizations.Menu.CourseSettingsPage1Params(
                dto.getId(),
                dto.getLocalizedTitle(),
                dto.getTitleId(),
                dto.getDescriptionId() != null ? dto.getDescriptionId().toString() : notAvailable,
                dto.getEndId() != null ? dto.getEndId().toString() : notAvailable,
                dto.getPaymentType(),
                dto.getLessonIds().size(),
                dto.getPrice() != null ? dto.getPrice().toString() : notAvailable,
                dto.getRefundStage() != null ? dto.getRefundStage().toString() : notAvailable,
                dto.getExternalStorePageUrl() != null ? dto.getExternalStorePageUrl() : notAvailable,
                dto.getExternalInvoiceMappingId() != null ? dto.getExternalInvoiceMappingId().toString() : notAvailable,
                dto.isUnderMaintenance() ? yes : no,   
                dto.isHomeworkIncluded() ? yes : no,   
                dto.isFeedbackIncluded() ? yes : no
            ));
        });
        secondPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            final Course course = entityUtil.getCourseById(p.botRole(), Long.parseLong(p.params().get(COURSE_ID_PARAM)));
            
            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.COURSE_NAME_SETTINGS, p.botRole()).getData(),
                List.of(MAPPING_ID_PARAM, TEXT_ONLY_PARAM), List.of(course.getTitle().getId().toString(), String.valueOf(true)), 7));
            if (course.getEndMapping() == null) {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.CREATE_COURSE_END_MAPPING, p.botRole()).getData(), createEndMappingHandler));
            } else {
                buttons.add(new TransitoryButton(loader.localize(Localizations.Button.COURSE_END_MAPPING_SETTINGS, p.botRole()).getData(),
                    List.of(MAPPING_ID_PARAM, TEXT_ONLY_PARAM), List.of(course.getEndMapping().getId().toString(), String.valueOf(false)), 7));
            }

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.COURSE_PRICE_CHANGE, p.botRole()).getData(), priceChangeHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.GIVE_OR_TAKE_COURSE, p.botRole()).getData(), giveOrTakeAwayCourseHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.COURSE_FEEDBACK_SETTING, p.botRole()).getData(), feedbackHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.COURSE_HOMEWORK_SETTING, p.botRole()).getData(), homeworkHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.TOGGLE_COURSE_MAINTENANCE, p.botRole()).getData(), courseMaintenanceToggleHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.UPDATE_REFUND_STAGE, p.botRole()).getData(), updateCourseRefundStageHandler));
            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.COURSE_LESSONS, p.botRole()).getData(), 2));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.REMOVE_COURSE, p.botRole()).getData(), removeCourseHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page thirdPage = new Page(menu);

        thirdPage.setPageIndex(2);
        thirdPage.setColumns(3);
        thirdPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_2, p.botRole()));
        thirdPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(lessonService.getCourseLessons(Long.parseLong(p.params().get(COURSE_ID_PARAM))).stream()
                .map(l -> (Button)new TransitoryButton(l.getPosition().toString(), LESSON_ID_PARAM, l.getId().toString(), 3)).toList());
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.CREATE_LESSON, p.botRole()).getData(), addLessonToCourseHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });
        
        final Page fourthPage = new Page(menu);

        fourthPage.setPageIndex(3);
        fourthPage.setColumns(1);
        fourthPage.setLocalizationFunction(p -> {
            final LessonResponseDto dto = lessonService.getById(p.botRole(), Long.parseLong(p.params().get(LESSON_ID_PARAM)));
            final String notAvailable = loader.localize(Localizations.Service.NOT_AVAILABLE, p.botRole()).getData();

            return loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_3, p.botRole(), new Localizations.Menu.CourseSettingsPage3Params(
                dto.getId(),
                dto.getPosition(),
                dto.getHomeworkId() != null ? dto.getHomeworkId().toString() : notAvailable,
                dto.getDelay() > 0 ? dto.getDelay().toString() : notAvailable,
                dto.getNextLessonButtonTitleMappingId() != null ? dto.getNextLessonButtonTitleMappingId().toString() : notAvailable,
                dto.getMappingIds()));            
        });
        fourthPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.LESSON_CONTENT_SETTINGS, p.botRole()).getData(), 6));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.SET_LESSON_DELAY, p.botRole()).getData(), lessonDelaySettingHandler));

            final LessonResponseDto dto = lessonService.getById(p.botRole(), Long.parseLong(p.params().get(LESSON_ID_PARAM)));

            final Button homeworkButton;
            if (dto.getHomeworkId() == null) {
                homeworkButton = new TerminalButton(loader.localize(Localizations.Button.CREATE_HOMEWORK, p.botRole()).getData(), createHomeworkHandler);
            } else {
                homeworkButton = new TransitoryButton(loader.localize(Localizations.Button.HOMEWORK_SETTINGS, p.botRole()).getData(),
                        HOMEWORK_ID_PARAM, dto.getHomeworkId().toString(), 4);
            }
            buttons.add(homeworkButton);

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.REMOVE_LESSON, p.botRole()).getData(), removeLessonFromCourseHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page fifthPage = new Page(menu);

        fifthPage.setPageIndex(4);
        fifthPage.setColumns(2);
        fifthPage.setLocalizationFunction(p -> {
            final HomeworkResponseDto dto = homeworkService.getById(p.botRole(), Long.parseLong(p.params().get(HOMEWORK_ID_PARAM)));
            final String notAvailable = loader.localize(Localizations.Service.NOT_AVAILABLE, p.botRole()).getData();
            final String yes = loader.localize(Localizations.Service.YES, p.botRole()).getData();
            final String no = loader.localize(Localizations.Service.NO, p.botRole()).getData();

            return loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_4, p.botRole(), new Localizations.Menu.CourseSettingsPage4Params(
                dto.getId(),
                dto.getLessonId(),
                dto.getDelay() > 0 ? dto.getDelay().toString() : notAvailable,
                dto.getMappingId(),
                !dto.getAllowedMediaTypes().isEmpty()
                    ? dto.getAllowedMediaTypes().stream()
                        .map(t -> t.toString().transform(s -> s.charAt(0) + s.substring(1).toLowerCase()))
                        .collect(Collectors.joining(", "))
                    : notAvailable,
                dto.isFeedbackRequired() ? yes : no,
                dto.isRepeatedCompletionAvailable() ? yes : no
            ));
        });
        fifthPage.setButtonsFunction(p -> {
            final HomeworkResponseDto dto = homeworkService.getById(p.botRole(), Long.parseLong(p.params().get(HOMEWORK_ID_PARAM)));

            return List.of(
                new TransitoryButton(loader.localize(Localizations.Button.HOMEWORK_CONTENT_SETTINGS, p.botRole()).getData(),
                    List.of(MAPPING_ID_PARAM, TEXT_ONLY_PARAM), List.of(dto.getMappingId().toString(), String.valueOf(false)), 7),
                new TerminalButton(loader.localize(Localizations.Button.UPDATE_MEDIA_TYPES, p.botRole()).getData(), homeworkMediaTypesHandler),
                new TerminalButton(loader.localize(Localizations.Button.SET_HOMEWORK_DELAY, p.botRole()).getData(), homeworkDelaySettingHandler),
                new TerminalButton(loader.localize(Localizations.Button.HOMEWORK_FEEDBACK, p.botRole()).getData(), homeworkFeedbackHandler),
                new TerminalButton(loader.localize(Localizations.Button.HOMEWORK_REPEATED_COMPLETION, p.botRole()).getData(), homeworkRepeatedCompletionHandler),
                new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData())
            );
        });

        final Page sixthPage = new Page(menu);

        sixthPage.setPageIndex(5);
        sixthPage.setColumns(2);
        sixthPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_5, p.botRole()));
        sixthPage.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.CREATE_COURSE_EXTERNAL_PAYMENT, p.botRole()).getData(),
                    PaymentType.EXTERNAL.toString(), createCourseHandler),
            new TerminalButton(loader.localize(Localizations.Button.CREATE_COURSE_TELEGRAM_PAYMENT, p.botRole()).getData(),
                    PaymentType.TELEGRAM.toString(), createCourseHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData())
        ));

        final Page seventhPage = new Page(menu);

        seventhPage.setPageIndex(6);
        seventhPage.setColumns(3);
        seventhPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_6, p.botRole()));
        seventhPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(contentService.getContentMappingsForLesson(Long.parseLong(p.params().get(LESSON_ID_PARAM))).stream()
                .map(cm -> (Button)new TransitoryButton(cm.getPosition().toString() + " (" + cm.getId() + ")",
                List.of(MAPPING_ID_PARAM, TEXT_ONLY_PARAM), List.of(cm.getId().toString(), String.valueOf(false)), 7)).toList());
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.ADD_CONTENT_TO_LESSON, p.botRole()).getData(), addContentToLessonHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page eigthPage = new Page(menu);

        eigthPage.setPageIndex(7);
        eigthPage.setColumns(3);
        eigthPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_7, p.botRole()));
        eigthPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(contentService.getContentForMapping(Long.parseLong(p.params().get(MAPPING_ID_PARAM))).stream()
                .map(c -> (Button)new TransitoryButton(c.getLanguageCode().toString() + " (" + c.getId() + ")",
                CONTENT_ID_PARAM, c.getId().toString(), 8)).toList());
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.ADD_MAPPING_LOCALIZATION, p.botRole()).getData(), addMappingLocalizationHandler));
            if (p.history().getLast().equals(6)) {
                buttons.add(new TransitoryButton(loader.localize(Localizations.Button.REMOVE_MAPPING_FROM_LESSON, p.botRole()).getData(), 10));
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.CHANGE_MAPPING_ORDER, p.botRole()).getData(), updateContentPositionHandler));
            } else if (p.history().getLast().equals(1)) {
                final Course course = entityUtil.getCourseById(p.botRole(), Long.parseLong(p.params().get(COURSE_ID_PARAM)));
                
                if (course.getEndMapping() != null) {
                    buttons.add(new TransitoryButton(loader.localize(Localizations.Button.REMOVE_COURSE_END_MAPPING, p.botRole()).getData(), 11));
                }
            }
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page ninthPage = new Page(menu);

        ninthPage.setPageIndex(8);
        ninthPage.setColumns(3);
        ninthPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_8, p.botRole()));
        ninthPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.REMOVE_MAPPING_LOCALIZATION, p.botRole()).getData(), 9));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.GET_CONTENT, p.botRole()).getData(), getContentHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page tenthPage = new Page(menu);

        tenthPage.setPageIndex(9);
        tenthPage.setColumns(2);
        tenthPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_9, p.botRole()));
        tenthPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(loader.localize(Localizations.Service.YES, p.botRole()).getData(), removeMappingLocalizationHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page eleventhPage = new Page(menu);

        eleventhPage.setPageIndex(10);
        eleventhPage.setColumns(2);
        eleventhPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_10, p.botRole()));
        eleventhPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(loader.localize(Localizations.Service.YES, p.botRole()).getData(), removeContentFromLessonHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page twelfthPage = new Page(menu);

        twelfthPage.setPageIndex(11);
        twelfthPage.setColumns(2);
        twelfthPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.COURSE_SETTINGS_PAGE_11, p.botRole()));
        twelfthPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(loader.localize(Localizations.Service.YES, p.botRole()).getData(), removeEndMappingHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        menu.setPages(List.of(
            firstPage, secondPage, thirdPage,
            fourthPage, fifthPage, sixthPage,
            seventhPage, eigthPage, ninthPage,
            tenthPage, eleventhPage, twelfthPage
        ));
        
        return menu;
    }
}

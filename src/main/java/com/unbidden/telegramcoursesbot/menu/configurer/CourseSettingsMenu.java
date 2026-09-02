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
import com.unbidden.telegramcoursesbot.menu.handler.CourseMaintenanceToggleButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CoursePriceChangeButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CreateCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CreateHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.FeedbackInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.GiveOrTakeAwayCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkDelaySettingButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkFeedbackToggleButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkMediaTypesChangeButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.HomeworkRepeatedCompletionToggleButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.LessonDelaySettingButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveContentFromLessonButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveLessonFromCourseButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.UpdateContentPositionButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.UpdateCourseRefundStageButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.UpdateHomeworkContentButtonHandler;
import com.unbidden.telegramcoursesbot.model.CourseInvoice.PaymentType;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.LessonOrchestrationService;

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
    private static final String COURSE_ID_PARAM = "courseId";

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
        secondPage.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.COURSE_PRICE_CHANGE, p.botRole()).getData(), priceChangeHandler),
            new TerminalButton(loader.localize(Localizations.Button.GIVE_OR_TAKE_COURSE, p.botRole()).getData(), giveOrTakeAwayCourseHandler),
            new TerminalButton(loader.localize(Localizations.Button.COURSE_FEEDBACK_SETTING, p.botRole()).getData(), feedbackHandler),
            new TerminalButton(loader.localize(Localizations.Button.COURSE_HOMEWORK_SETTING, p.botRole()).getData(), homeworkHandler),
            new TerminalButton(loader.localize(Localizations.Button.TOGGLE_COURSE_MAINTENANCE, p.botRole()).getData(), courseMaintenanceToggleHandler),
            new TerminalButton(loader.localize(Localizations.Button.UPDATE_REFUND_STAGE, p.botRole()).getData(), updateCourseRefundStageHandler),
            new TransitoryButton(loader.localize(Localizations.Button.COURSE_LESSONS, p.botRole()).getData(), 2),
            new TerminalButton(loader.localize(Localizations.Button.REMOVE_COURSE, p.botRole()).getData(), removeCourseHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData())
        ));

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
        fourthPage.setColumns(2);
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

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.ADD_CONTENT_TO_LESSON, p.botRole()).getData(), addContentToLessonHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.REMOVE_CONTENT_FROM_LESSON, p.botRole()).getData(), removeContentFromLessonHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.CHANGE_MAPPING_ORDER, p.botRole()).getData(), updateContentPositionHandler));
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
        fifthPage.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.UPDATE_HOMEWORK_CONTENT, p.botRole()).getData(), updateHomeworkContentHandler),
            new TerminalButton(loader.localize(Localizations.Button.UPDATE_MEDIA_TYPES, p.botRole()).getData(), homeworkMediaTypesHandler),
            new TerminalButton(loader.localize(Localizations.Button.SET_HOMEWORK_DELAY, p.botRole()).getData(), homeworkDelaySettingHandler),
            new TerminalButton(loader.localize(Localizations.Button.HOMEWORK_FEEDBACK, p.botRole()).getData(), homeworkFeedbackHandler),
            new TerminalButton(loader.localize(Localizations.Button.HOMEWORK_REPEATED_COMPLETION, p.botRole()).getData(), homeworkRepeatedCompletionHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData())
        ));

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

        menu.setPages(List.of(firstPage, secondPage, thirdPage, fourthPage, fifthPage, sixthPage));
        
        return menu;
    }
}

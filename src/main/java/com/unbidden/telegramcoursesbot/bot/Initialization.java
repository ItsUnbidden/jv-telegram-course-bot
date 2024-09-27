package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.model.CourseModel;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.service.button.handler.AddOrRemoveAdminButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.CoursePriceChangeButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.FeedbackInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.GiveOrTakeAwayCourseButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.HomeworkInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.ListAdminsButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Type;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;

@Component
@RequiredArgsConstructor
public class Initialization implements ApplicationRunner {
    private final CoursePriceChangeButtonHandler priceChangeHandler;
    private final FeedbackInclusionButtonHandler feedbackHandler;
    private final HomeworkInclusionButtonHandler homeworkHandler;
    private final AddOrRemoveAdminButtonHandler addOrRemoveAdminHandler;
    private final ListAdminsButtonHandler listAdminsHandler;
    private final GiveOrTakeAwayCourseButtonHandler giveOrTakeAwayCourseHandler;

    private final LocalizationLoader localizationLoader;

    private final CourseRepository courseRepository;

    private final MenuRepository menuRepository;

    private final TelegramBotsApi api;

    private final TelegramBot bot;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        setUpCourseSettingsMenu();
        setUpAdminActionsMenu();

        api.registerBot(bot);
    }

    private void setUpCourseSettingsMenu() {
        final Menu courseSettingsMenu = new Menu();
        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setType(Type.TRANSITORY);
        firstPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                "menu_course_settings_page_0", u));
        firstPage.setMenu(courseSettingsMenu);
        firstPage.setButtonsFunction(u -> courseRepository.findAll().stream()
                .map(cm -> (Button)new TransitoryButton(localizationLoader
                    .getLocalizationForUser(cm.getLocFileCourseName(), u).getData(),
                    cm.getName()))
                .toList());

        final Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setType(Type.TERMINAL);
        secondPage.setLocalizationFunction((u, p) -> {
            final CourseModel course = courseRepository.findByName(p.get(2)).orElseThrow(() ->
                new EntityNotFoundException("Page 1 in menu " + courseSettingsMenu.getName()
                + " is not able to user the text function since the chosen course does "
                + "not exist anymore."));
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("${id}", course.getId());
            parameterMap.put("${name}", course.getName());
            parameterMap.put("${sequenceMode}", course.getSequenceOption().toString());
            parameterMap.put("${numberOfLessons}", course.getAmountOfLessons());
            parameterMap.put("${price}", course.getPrice());
            parameterMap.put("${isHomeworkIncluded}", course.isHomeworkIncluded());
            parameterMap.put("${isFeedbackIncluded}", course.isFeedbackIncluded());

            return localizationLoader.getLocalizationForUser(
                "menu_course_settings_page_1", u, parameterMap);
        });
        secondPage.setMenu(courseSettingsMenu);
        secondPage.setButtonsFunction(u -> List.of(new TerminalButton(
            localizationLoader.getLocalizationForUser("button_course_price_change", u).getData(),
            "prCh", priceChangeHandler), new TerminalButton(
            localizationLoader.getLocalizationForUser("button_give_or_take_course", u).getData(),
            "gtC", giveOrTakeAwayCourseHandler), new TerminalButton(
            localizationLoader.getLocalizationForUser("button_course_feedback_setting", u)
                .getData(), "fb", feedbackHandler), new TerminalButton(
            localizationLoader.getLocalizationForUser("button_course_homework_setting", u)
                .getData(), "hw", homeworkHandler)));

        courseSettingsMenu.setName("m_crsOpt");
        courseSettingsMenu.setPages(List.of(firstPage, secondPage));
        menuRepository.save(courseSettingsMenu);
    }

    private void setUpAdminActionsMenu() {
        final Menu adminActionsMenu = new Menu();
        final Page page = new Page();
        page.setMenu(adminActionsMenu);
        page.setPageIndex(0);
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            "menu_admin_actions_page_0", u));
        page.setType(Type.TERMINAL);
        page.setButtonsFunction(u -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser("button_add_or_remove_admin", u)
                    .getData(), "arA", addOrRemoveAdminHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser("button_list_admins", u)
                    .getData(), "lA", listAdminsHandler)));
        adminActionsMenu.setName("m_admAct");
        adminActionsMenu.setPages(List.of(page));
        menuRepository.save(adminActionsMenu);
    }
}

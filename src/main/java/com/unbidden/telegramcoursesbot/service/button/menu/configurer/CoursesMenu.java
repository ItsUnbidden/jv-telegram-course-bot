package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class CoursesMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_crs";

    private static final String BEGIN_COURSE = "bC";
    private static final String MY_COURSES = "mC";
    private static final String AVAILABLE_COURSES = "aC";

    private static final String BUTTON_LEAVE_REVIEW = "button_leave_review";
    private static final String BUTTON_BEGIN_COURSE = "button_begin_course";
    private static final String BUTTON_MY_COURSES = "button_my_courses";
    private static final String BUTTON_AVAILABLE_COURSES = "button_available_courses";

    private static final String COURSE_NAME = "course_%s_name";

    private static final String MENU_COURSES_PAGE_3 = "menu_courses_page_3";
    private static final String MENU_COURSES_PAGE_2 = "menu_courses_page_2";
    private static final String MENU_COURSES_PAGE_1 = "menu_courses_page_1";
    private static final String MENU_COURSES_PAGE_0 = "menu_courses_page_0";

    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final MenuService menuService;

    private final TelegramBot bot;
    
    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setButtonsRowSize(2);
        firstPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_COURSES_PAGE_0, u));
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction((u, p) -> List.of(new TransitoryButton(localizationLoader
                .getLocalizationForUser(BUTTON_AVAILABLE_COURSES, u).getData(),
                AVAILABLE_COURSES, 1), new TransitoryButton(localizationLoader
                .getLocalizationForUser(BUTTON_MY_COURSES, u).getData(), MY_COURSES, 2)));
        final Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setButtonsRowSize(2);
        secondPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_COURSES_PAGE_1, u));
        secondPage.setMenu(menu);
        secondPage.setButtonsFunction((u, p) -> {
            final List<String> ownedCoursesNames = courseService.getAllOwnedByUser(u).stream()
                    .map(c -> c.getName()).toList();
            final List<String> allCoursesNames = courseService.getAll().stream()
                    .map(c -> c.getName()).toList();
            return allCoursesNames.stream().filter(cn -> !ownedCoursesNames.contains(cn))
                    .map(cn -> (Button)new TerminalButton(localizationLoader
                    .getLocalizationForUser(COURSE_NAME.formatted(cn), u).getData(), cn,
                    (p1, u1) -> courseService.initMessage(u, cn))).toList();
        });
        final Page thirdPage = new Page();
        thirdPage.setPageIndex(2);
        thirdPage.setButtonsRowSize(2);
        thirdPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_COURSES_PAGE_2, u));
        thirdPage.setMenu(menu);
        thirdPage.setButtonsFunction((u, p) -> {
            final List<Course> allOwnedByUser = courseService.getAllOwnedByUser(u);

            return allOwnedByUser.stream()
                    .map(c ->(Button)new TransitoryButton(localizationLoader
                    .getLocalizationForUser(COURSE_NAME.formatted(c.getName()), u).getData(),
                    c.getName(), 3)).toList();
        });
        final Page fourthPage = new Page();
        fourthPage.setPageIndex(3);
        fourthPage.setButtonsRowSize(2);
        fourthPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            MENU_COURSES_PAGE_3, u));
        fourthPage.setMenu(menu);
        fourthPage.setButtonsFunction((u, p) -> List.of(new TerminalButton(localizationLoader
                .getLocalizationForUser(BUTTON_BEGIN_COURSE, u).getData(), BEGIN_COURSE,
                (u1, pa) -> courseService.initMessage(u1, pa[1])), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_LEAVE_REVIEW, u).getData(),
                "lR", (u1, pa) -> bot.sendMessage(SendMessage.builder().chatId(u1.getId())
                .text("Reviews are currently not implemented.").build()))));
                // TODO: implement leaving reviews
        menu.setName(MENU_NAME);
        menu.setPages(List.of(firstPage, secondPage, thirdPage, fourthPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setAttachedToMessage(false);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
        // TODO: add review options here
    }
}

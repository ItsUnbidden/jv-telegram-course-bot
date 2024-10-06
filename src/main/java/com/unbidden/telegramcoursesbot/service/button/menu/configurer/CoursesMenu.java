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
    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final MenuService menuService;

    private final TelegramBot bot;
    
    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            "menu_courses_page_0", u));
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction(u -> List.of(new TransitoryButton(localizationLoader
                .getLocalizationForUser("button_available_courses", u).getData(), "aC", 1),
                new TransitoryButton(localizationLoader.getLocalizationForUser(
                "button_my_courses", u).getData(), "mC", 2)));
        final Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            "menu_courses_page_1", u));
        secondPage.setMenu(menu);
        secondPage.setButtonsFunction(u -> {
            final List<String> ownedCoursesNames = courseService.getAllOwnedByUser(u).stream()
                    .map(c -> c.getName()).toList();
            final List<String> allCoursesNames = courseService.getAll().stream()
                    .map(c -> c.getName()).toList();
            return allCoursesNames.stream().filter(cn -> !ownedCoursesNames.contains(cn))
                    .map(cn -> (Button)new TerminalButton(localizationLoader
                    .getLocalizationForUser("course_" + cn + "_name", u).getData(), cn,
                    (p1, u1) -> courseService.initMessage(u, cn))).toList();
        });
        final Page thirdPage = new Page();
        thirdPage.setPageIndex(2);
        thirdPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            "menu_courses_page_2", u));
        thirdPage.setMenu(menu);
        thirdPage.setButtonsFunction(u -> {
            final List<Course> allOwnedByUser = courseService.getAllOwnedByUser(u);

            return allOwnedByUser.stream()
                    .map(c ->(Button)new TransitoryButton(localizationLoader
                    .getLocalizationForUser("course_" + c.getName() + "_name", u).getData(),
                    c.getName(), 3)).toList();
        });
        final Page fourthPage = new Page();
        fourthPage.setPageIndex(3);
        fourthPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            "menu_courses_page_3", u));
        fourthPage.setMenu(menu);
        fourthPage.setButtonsFunction(u -> List.of(new TerminalButton(localizationLoader
                .getLocalizationForUser("button_begin_course", u).getData(), "bC",
                (p1, u1) -> courseService.initMessage(u1, p1[1])), new TerminalButton(
                localizationLoader.getLocalizationForUser("button_leave_review", u).getData(),
                "lR", (p1, u1) -> bot.sendMessage(SendMessage.builder().chatId(u1.getId())
                .text("Reviews are currently not implemented.").build()))));
                // TODO: implement leaving reviews
        menu.setName("m_crs");
        menu.setPages(List.of(firstPage, secondPage, thirdPage, fourthPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menu.setAttachedToMessage(false);
        menuService.save(menu);
    }
}

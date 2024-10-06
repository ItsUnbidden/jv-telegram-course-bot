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
public class MyCoursesMenu implements MenuConfigurer {
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
            "menu_my_courses_page_0", u));
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction(u -> {
            final List<Course> allOwnedByUser = courseService.getAllOwnedByUser(u);

            return allOwnedByUser.stream()
                    .map(c ->(Button)new TransitoryButton(localizationLoader
                    .getLocalizationForUser("course_" + c.getName() + "_name", u).getData(),
                    c.getName(), 1)).toList();
        });
        final Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
            "menu_my_courses_page_1", u));
        secondPage.setMenu(menu);
        secondPage.setButtonsFunction(u -> List.of(new TerminalButton(localizationLoader
                .getLocalizationForUser("button_begin_course", u).getData(), "bC",
                (p1, u1) -> courseService.initMessage(u1, p1[0])), new TerminalButton(
                localizationLoader.getLocalizationForUser("button_leave_review", u).getData(),
                "lR", (p1, u1) -> bot.sendMessage(SendMessage.builder().chatId(u1.getId())
                .text("Reviews are currently not implemented.").build()))));
                // TODO: implement leaving reviews
        menu.setName("m_myCrs");
        menu.setPages(List.of(firstPage, secondPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menu.setAttachedToMessage(false);
        menuService.save(menu);
    }
}

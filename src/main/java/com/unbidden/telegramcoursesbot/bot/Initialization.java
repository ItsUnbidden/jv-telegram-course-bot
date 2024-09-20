package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.LocalizationLoader;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;
import com.unbidden.telegramcoursesbot.service.button.handler.CoursePriceChangeButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.FeedbackInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.handler.HomeworkInclusionButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Type;
import java.util.List;
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

    private final LocalizationLoader localizationLoader;

    private final CourseRepository courseRepository;

    private final MenuRepository menuRepository;

    private final TelegramBotsApi api;

    private final TelegramBot bot;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Menu menu = new Menu();
        Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setType(Type.TRANSITORY);
        firstPage.setTextFunction((u) -> localizationLoader.getTextByNameForUser(
            "message_course_settings_page_0", u));
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction((u) -> courseRepository.findAll().stream()
            .map(cm -> (Button)new TransitoryButton(localizationLoader
                .getTextByNameForUser(cm.getLocFileCourseName(), u), cm.getName()))
            .toList());

        Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setType(Type.TERMINAL);
        secondPage.setTextFunction((u) -> localizationLoader.getTextByNameForUser(
            "message_course_settings_page_1", u));
        secondPage.setMenu(menu);
        secondPage.setButtonsFunction((u) -> List.of(new TerminalButton(
            localizationLoader.getTextByNameForUser("button_course_price_change", u),
            "prCh", priceChangeHandler), new TerminalButton(
            localizationLoader.getTextByNameForUser("button_course_feedback", u),
            "fb", feedbackHandler), new TerminalButton(
            localizationLoader.getTextByNameForUser("button_course_homework", u),
            "hw", homeworkHandler)));

        menu.setName("m_crsOpt");
        menu.setPages(List.of(firstPage, secondPage));
        menuRepository.save(menu);

        api.registerBot(bot);
    }
}

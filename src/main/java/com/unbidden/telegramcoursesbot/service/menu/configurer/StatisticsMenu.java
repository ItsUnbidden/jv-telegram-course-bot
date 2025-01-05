package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.StatisticsButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatisticsMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_stat";
    
    private static final String BUTTON_COURSE_STATISTICS = "button_course_statistics";
    private static final String BUTTON_BOT_USERS_STATISTICS = "button_bot_users_statistics";
    private static final String BUTTON_COURSE_USERS_STATISTICS = "button_course_users_statistics";
    private static final String BUTTON_GENERAL_BOT_STATISTICS = "button_general_bot_statistics";
    private static final String BUTTON_COURSE_USERS_BY_STAGE = "button_course_users_by_stage";
    private static final String BUTTON_COURSE_COMPLETED_USERS = "button_course_completed_users";
    private static final String BUTTON_COURSE_ALL_USERS = "button_course_all_users";
    private static final String BUTTON_BACK = "button_back";
    
    private static final String COURSE_USERS_BY_STAGE = "cubs";
    private static final String COURSE_COMPLETED_USERS = "ccu";
    private static final String COURSE_ALL_USERS = "cau";
    private static final String COURSE_USERS_STATISTICS = "cus";
    private static final String COURSE_STATISTICS = "cs";
    private static final String BOT_USERS_STATISTICS = "bus";
    private static final String GENERAL_BOT_STATISTICS = "gbs";

    private static final String MENU_STATISTICS_PAGE_0 = "menu_statistics_page_0";
    private static final String MENU_STATISTICS_PAGE_1 = "menu_statistics_page_1";
    private static final String MENU_STATISTICS_PAGE_2 = "menu_statistics_page_2";
    private static final String MENU_STATISTICS_PAGE_3 = "menu_statistics_page_3";

    private static final String COURSE_NAME = "course_%s_name";

    private final StatisticsButtonHandler statisticsHandler;

    private final MenuService menuService;

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page1 = new Page();
        page1.setMenu(menu);
        page1.setPageIndex(0);
        page1.setButtonsRowSize(1);
        page1.setLocalizationFunction((u, p, b) -> localizationLoader
                .getLocalizationForUser(MENU_STATISTICS_PAGE_0, u));
        page1.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            buttons.addAll(courseService.getByBot(b).stream()
                .map(c -> (Button)new TransitoryButton(localizationLoader
                    .getLocalizationForUser(COURSE_NAME.formatted(c.getName()), u).getData(),
                    c.getName(), 1))
                .toList());

            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_GENERAL_BOT_STATISTICS, u).getData(), GENERAL_BOT_STATISTICS,
                statisticsHandler));
                
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_BOT_USERS_STATISTICS, u).getData(), BOT_USERS_STATISTICS,
                statisticsHandler));
            return buttons;
        });

        final Page page2 = new Page();
        page2.setMenu(menu);
        page2.setPageIndex(1);
        page2.setButtonsRowSize(1);
        page2.setPreviousPage(0);
        page2.setLocalizationFunction((u, p, b) -> localizationLoader
                .getLocalizationForUser(MENU_STATISTICS_PAGE_1, u));
        page2.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_COURSE_STATISTICS, u).getData(), COURSE_STATISTICS,
                    statisticsHandler));

            buttons.add(new TransitoryButton(localizationLoader.getLocalizationForUser(
                    BUTTON_COURSE_USERS_STATISTICS, u).getData(), COURSE_USERS_STATISTICS,
                    2));

            buttons.add(new BackwardButton(localizationLoader.getLocalizationForUser(
                    BUTTON_BACK, u).getData()));
            return buttons;
        });

        final Page page3 = new Page();
        page3.setMenu(menu);
        page3.setPageIndex(2);
        page3.setButtonsRowSize(2);
        page3.setPreviousPage(1);
        page3.setLocalizationFunction((u, p, b) -> localizationLoader
                .getLocalizationForUser(MENU_STATISTICS_PAGE_2, u));
        page3.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            
            buttons.add(new TransitoryButton(localizationLoader.getLocalizationForUser(
                    BUTTON_COURSE_USERS_BY_STAGE, u).getData(), COURSE_USERS_BY_STAGE, 3));

            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_COURSE_COMPLETED_USERS, u).getData(), COURSE_COMPLETED_USERS,
                    statisticsHandler));

            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_COURSE_ALL_USERS, u).getData(), COURSE_ALL_USERS,
                    statisticsHandler));

            buttons.add(new BackwardButton(localizationLoader.getLocalizationForUser(
                    BUTTON_BACK, u).getData()));
            return buttons;
        });

        final Page page4 = new Page();
        page4.setMenu(menu);
        page4.setPageIndex(3);
        page4.setButtonsRowSize(3);
        page4.setPreviousPage(2);
        page4.setLocalizationFunction((u, p, b) -> localizationLoader
                .getLocalizationForUser(MENU_STATISTICS_PAGE_3, u));
        page4.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            
            buttons.addAll(courseService.getCourseByName(p.get(0), u, b).getLessons().stream()
                    .map(l -> new TerminalButton(l.getPosition().toString(),
                        l.getPosition().toString(), statisticsHandler))
                    .toList());

            buttons.add(new BackwardButton(localizationLoader.getLocalizationForUser(
                    BUTTON_BACK, u).getData()));
            return buttons;
        });
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page1, page2, page3, page4));
        menuService.save(menu);
    }
}

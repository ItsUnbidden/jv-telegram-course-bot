package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.service.button.handler.NextStageButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseNextStageMenu implements MenuConfigurer {
    private final NextStageButtonHandler nextStageHandler;

    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final MenuService menuService;
    
    @Override
    public void configure() {
        final Menu courseNextStageMenu = new Menu();
        final Page page = new Page();
        page.setMenu(courseNextStageMenu);
        page.setPageIndex(0);
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                "menu_" + courseService.getCourseByName(p.get(0)).getName()
                + "_next_stage_page_0", u));
        page.setButtonsFunction(u -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser("button_course_next_stage", u)
                .getData(), "ns", nextStageHandler)));
        final Page terminalPage = new Page();
        terminalPage.setMenu(courseNextStageMenu);
        terminalPage.setPageIndex(1);
        terminalPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                "menu_" + courseService.getCourseByName(p.get(0)).getName()
                + "_next_stage_terminal_page", u));
        courseNextStageMenu.setName("m_crsNxtStg");
        courseNextStageMenu.setPages(List.of(page, terminalPage));
        courseNextStageMenu.setInitialParameterPresent(true);
        courseNextStageMenu.setOneTimeMenu(true);
        courseNextStageMenu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(courseNextStageMenu);
    }
}

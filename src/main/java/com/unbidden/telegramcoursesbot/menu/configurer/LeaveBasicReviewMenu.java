package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.dto.CourseResponseDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.SendBasicReviewButtonHandler;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveBasicReviewMenu implements MenuConfigurer {
    private static final String COURSE_ID_PARAM = "courseId";

    private static final List<Button> COURSE_GRADE_BUTTONS = new ArrayList<>();
    private static final int GRADE_OPTIONS = 10;

    private final SendBasicReviewButtonHandler sendBasicReviewHandler;

    private final CourseOrchestrationService courseService;

    private final LocalizationLoader loader;

    @PostConstruct
    public void init() {
        for (int i = 1; i <= GRADE_OPTIONS; i++) {
            final String iSrt = String.valueOf(i);

            COURSE_GRADE_BUTTONS.add(new TerminalButton(iSrt, iSrt, sendBasicReviewHandler));
        }
    }

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.LEAVE_BASIC_REVIEW);

        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setColumns(5);
        firstPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.LEAVE_BASIC_REVIEW_PAGE_0, p.user()));
        firstPage.setButtonsFunction(p -> COURSE_GRADE_BUTTONS);

        final Page terminalPage = new Page(menu);

        terminalPage.setPageIndex(2);
        terminalPage.setLocalizationFunction(p -> {
            final CourseResponseDto dto = courseService.getById(p.user(), p.bot(), Long.parseLong(p.params().get(COURSE_ID_PARAM)));

            return loader.localize(Localizations.Menu.LEAVE_BASIC_REVIEW_TERMINAL_PAGE, p.user(),
                    new Localizations.Menu.LeaveBasicReviewTerminalPageParams(dto.getLocalizedTitle()));
        });

        menu.setTerminalPage(terminalPage);
        menu.setPages(List.of(firstPage, terminalPage));

        return menu;
    }
}

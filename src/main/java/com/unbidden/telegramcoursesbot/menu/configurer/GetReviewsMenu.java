package com.unbidden.telegramcoursesbot.menu.configurer;

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
import com.unbidden.telegramcoursesbot.menu.handler.GetArchiveReviewsButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.GetNewReviewsButtonHandler;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetReviewsMenu implements MenuConfigurer {
    private static final String COURSE_ID_PARAM = "courseId";

    private final GetArchiveReviewsButtonHandler getArchiveReviewsHandler;
    private final GetNewReviewsButtonHandler getNewReviewsHandler;

    private final CourseOrchestrationService courseService;

    private final LocalizationLoader loader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.GET_REVIEWS);

        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setColumns(1);
        firstPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.GET_REVIEWS_PAGE_0, p.user()));
        firstPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(courseService.getByBot(p.user(), p.bot()).stream()
                .map(c -> (Button)new TransitoryButton(c.getLocalizedTitle(), COURSE_ID_PARAM, c.getId().toString(), 1))
                .toList());
            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.ALL_COURSES_REVIEWS, p.user()).getData(), 1));

            return buttons;
        });

        final Page secondPage = new Page(menu);

        secondPage.setPageIndex(1);
        secondPage.setColumns(2);
        secondPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.GET_REVIEWS_PAGE_1, p.user()));
        secondPage.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.GET_NEW_REVIEWS, p.user()).getData(), getNewReviewsHandler),
            new TerminalButton(loader.localize(Localizations.Button.GET_ARCHIVE_REVIEWS, p.user()).getData(), getArchiveReviewsHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.user()).getData())
        ));

        menu.setPages(List.of(firstPage, secondPage));

        return menu;
    }
}

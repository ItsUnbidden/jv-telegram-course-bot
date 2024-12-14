package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.GetArchiveReviewsButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.GetNewReviewsButtonHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetReviewsMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_gRv";

    private static final String GET_ARCHIVE_REVIEWS = "gar";
    private static final String GET_NEW_REVIEWS = "gnr";
    private static final String ALL_COURSES = "-1";

    private static final String SERVICE_GET_ARCHIVE_REVIEWS = "service_get_archive_reviews";
    private static final String SERVICE_GET_NEW_REVIEWS = "service_get_new_reviews";

    private static final String BUTTON_ALL_COURSES_REVIEWS = "button_all_courses_reviews";
    private static final String BUTTON_BACK = "button_back";

    private static final String COURSE_NAME = "course_%s_name";

    private static final String MENU_GET_REVIEWS_PAGE_1 = "menu_get_reviews_page_1";
    private static final String MENU_GET_REVIEWS_PAGE_0 = "menu_get_reviews_page_0";

    private final GetArchiveReviewsButtonHandler getArchiveReviewsHandler;
    private final GetNewReviewsButtonHandler getNewReviewsHandler;

    private final MenuService menuService;

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;
    
    @Override
    public void configure() {
        final Menu menu = new Menu();

        final Page firstPage = new Page();
        firstPage.setPageIndex(0);
        firstPage.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
            MENU_GET_REVIEWS_PAGE_0, u));
        firstPage.setButtonsRowSize(1);
        firstPage.setMenu(menu);
        firstPage.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            buttons.addAll(courseService.getByBot(b).stream().map(c ->
                    (Button)new TransitoryButton(localizationLoader.getLocalizationForUser(
                    COURSE_NAME.formatted(c.getName()), u).getData(), c.getId().toString(), 1))
                    .toList());
            buttons.add(new TransitoryButton(localizationLoader.getLocalizationForUser(
                    BUTTON_ALL_COURSES_REVIEWS, u).getData(), ALL_COURSES, 1));
            return buttons;
        });

        final Page secondPage = new Page();
        secondPage.setPageIndex(1);
        secondPage.setPreviousPage(0);
        secondPage.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
            MENU_GET_REVIEWS_PAGE_1, u));
        secondPage.setButtonsRowSize(2);
        secondPage.setMenu(menu);
        secondPage.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(localizationLoader
                .getLocalizationForUser(SERVICE_GET_NEW_REVIEWS, u).getData(), GET_NEW_REVIEWS,
                getNewReviewsHandler), new TerminalButton(localizationLoader
                .getLocalizationForUser(SERVICE_GET_ARCHIVE_REVIEWS, u).getData(),
                GET_ARCHIVE_REVIEWS, getArchiveReviewsHandler), new BackwardButton(
                localizationLoader.getLocalizationForUser(BUTTON_BACK, u).getData())));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(firstPage, secondPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }

}

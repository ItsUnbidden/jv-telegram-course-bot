package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.handler.SendSupportRequestButtonHandler;
import com.unbidden.telegramcoursesbot.service.support.SupportService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportRequestMenu implements MenuConfigurer{
    private static final String MENU_NAME = "m_sr";

    private static final String BUTTON_SUPPORT_REQUEST_COURSE = "button_support_request_course";
    private static final String BUTTON_SUPPORT_REQUEST_PLATFORM =
            "button_support_request_platform";
    private static final String BUTTON_RESOLVE_LAST_SUPPORT_REQUEST =
            "button_resolve_last_support_request";
    private static final String BUTTON_BACK = "button_back";
    
    private static final String REQUEST_SUPPORT_PLATFORM = "rsp";
    private static final String REQUEST_SUPPORT_COURSE = "rsc";
    private static final String RESOLVE_LAST_SUPPORT_REQUEST = "rlsr";

    private static final String MENU_SUPPORT_REQUEST_TERMINAL_PAGE =
            "menu_support_request_terminal_page";
    private static final String MENU_SUPPORT_REQUEST_PAGE_1 = "menu_support_request_page_1";
    private static final String MENU_SUPPORT_REQUEST_PAGE_0_NO_COURSES =
            "menu_support_request_page_0_no_courses";
    private static final String MENU_SUPPORT_REQUEST_PAGE_0 = "menu_support_request_page_0";

    private static final String COURSE_NAME = "course_%s_name";

    private final SendSupportRequestButtonHandler sendSupportRequestHandler;

    private final MenuService menuService;

    private final CourseService courseService;

    private final SupportService supportService;

    private final LocalizationLoader localizationLoader;
    
    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page1 = new Page();
        page1.setMenu(menu);
        page1.setPageIndex(0);
        page1.setButtonsRowSize(2);
        page1.setLocalizationFunction((u, p, b) -> {
            return (courseService.getAllOwnedByUser(u, b).isEmpty())
                ? localizationLoader.getLocalizationForUser(
                    MENU_SUPPORT_REQUEST_PAGE_0_NO_COURSES, u)
                : localizationLoader.getLocalizationForUser(MENU_SUPPORT_REQUEST_PAGE_0, u);
        });
        page1.setButtonsFunction((u, p, b) -> {
            final List<Button> buttons = new ArrayList<>();
            if (!courseService.getAllOwnedByUser(u, b).isEmpty()) {
                buttons.add(new TransitoryButton(localizationLoader.getLocalizationForUser(
                    BUTTON_SUPPORT_REQUEST_COURSE, u).getData(), REQUEST_SUPPORT_COURSE, 1));
            }
            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_SUPPORT_REQUEST_PLATFORM, u).getData(), REQUEST_SUPPORT_PLATFORM,
                sendSupportRequestHandler));
            if (!supportService.isUserEligibleForSupport(u, b)) {
                final SupportRequest lastUserRequest = supportService.getRequestById(
                        supportService.getUnresolvedRequestsForUser(u, b).get(0).getId(), u, b);
                buttons.add(new TerminalButton(BUTTON_RESOLVE_LAST_SUPPORT_REQUEST,
                        RESOLVE_LAST_SUPPORT_REQUEST, (b1, u1, pa) -> supportService
                        .markAsResolved(u1, b1, lastUserRequest)));
            }
            return buttons;
        });
        final Page page2 = new Page();
        page2.setMenu(menu);
        page2.setPageIndex(1);
        page2.setButtonsRowSize(2);
        page2.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
                MENU_SUPPORT_REQUEST_PAGE_1, u));
        page2.setButtonsFunction((u, p, b) -> {
            final List<Course> allOwnedByUser = courseService.getAllOwnedByUser(u, b);
            final List<Button> buttons = new ArrayList<>();

            for (Course course : allOwnedByUser) {
                buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    COURSE_NAME.formatted(course.getName()), u).getData(),
                    course.getId().toString(), sendSupportRequestHandler));
            }
            buttons.add(new BackwardButton(localizationLoader.getLocalizationForUser(
                    BUTTON_BACK, u).getData()));
            return buttons;
        });

        final Page terminalPage = new Page();
        terminalPage.setMenu(menu);
        terminalPage.setPageIndex(2);
        terminalPage.setLocalizationFunction((u, p, b) -> localizationLoader
                .getLocalizationForUser(MENU_SUPPORT_REQUEST_TERMINAL_PAGE, u));

        menu.setName(MENU_NAME);
        menu.setPages(List.of(page1, page2, terminalPage));
        menu.setOneTimeMenu(true);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

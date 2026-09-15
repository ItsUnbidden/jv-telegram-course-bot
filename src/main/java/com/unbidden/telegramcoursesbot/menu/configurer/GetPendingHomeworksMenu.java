package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.GetPendingHomeworksButtonHandler;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetPendingHomeworksMenu implements MenuConfigurer {
    private final GetPendingHomeworksButtonHandler getPendingHomeworksHandler;

    private final CourseOrchestrationService courseService;

    private final LocalizationLoader loader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.GET_PENDING_HOMEWORKS);

        final Page firstPage = new Page(menu);

        firstPage.setPageIndex(0);
        firstPage.setColumns(1);
        firstPage.setLocalizationFunction(p -> loader.localize(Localizations.Menu.GET_REVIEWS_PAGE_0, p.botRole()));
        firstPage.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(courseService.getByBot(p.botRole()).stream()
                .map(c -> (Button)new TerminalButton(c.getLocalizedTitle(), c.getId().toString(), getPendingHomeworksHandler))
                .toList());
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.ALL_COURSES_REVIEWS, p.botRole()).getData(), getPendingHomeworksHandler));

            return buttons;
        });

        menu.setPages(List.of(firstPage));

        return menu;
    }
}

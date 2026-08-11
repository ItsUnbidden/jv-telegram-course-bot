package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.CourseNextStageButtonHandler;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseNextStageMenu implements MenuConfigurer {
    private final CourseNextStageButtonHandler courseNextStageHandler;

    private final ContentService contentService;

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.COURSE_NEXT_STAGE);

        final Page page = new Page(menu);
        
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p, b) -> {
            final Lesson lesson = entityUtil.getLessonById(u, b, Long.parseLong(p.get(0)));
            final String buttonName = lesson.getNextLessonButtonTitle() == null
                    ? loader.localize(Localizations.Button.NEXT_LESSON_DEFAULT, u).getData()
                    : contentService.getLocalizedText(u, b, lesson.getNextLessonButtonTitle().getId());

            return List.of(new TerminalButton(buttonName, lesson.getCourse().getId().toString(), courseNextStageHandler));
        });

        final Page terminalPage = new Page(menu);

        terminalPage.setPageIndex(1);

        menu.setPages(List.of(page, terminalPage));
        menu.setInitialParameterPresent(true);
        menu.setAttachedToMessage(true);

        return menu;
    }
}

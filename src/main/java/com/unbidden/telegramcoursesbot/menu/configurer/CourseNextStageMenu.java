package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.CourseNextStageButtonHandler;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseNextStageMenu implements MenuConfigurer {
    private static final String LESSON_ID_PARAM = "lessonId";

    private final CourseNextStageButtonHandler courseNextStageHandler;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.COURSE_NEXT_STAGE);

        final Page page = new Page(menu);
        
        page.setPageIndex(0);
        page.setColumns(1);
        page.setButtonsFunction(p -> {
            final Lesson lesson = entityUtil.getLessonById(p.user(), p.bot(), Long.parseLong(p.params().get(LESSON_ID_PARAM)));
            final String buttonName = lesson.getNextLessonButtonTitle() == null
                    ? loader.localize(Localizations.Button.NEXT_LESSON_DEFAULT, p.user()).getData()
                    : contentService.getLocalizedText(p.user(), p.bot(), lesson.getNextLessonButtonTitle().getId());

            return List.of(new TerminalButton(buttonName, lesson.getCourse().getId().toString(), courseNextStageHandler));
        });

        menu.setPages(List.of(page));

        return menu;
    }
}

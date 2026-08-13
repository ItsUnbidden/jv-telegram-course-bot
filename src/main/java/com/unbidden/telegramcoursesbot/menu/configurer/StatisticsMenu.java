package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.dto.internal.UsersByCourseStageCountDto;
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
import com.unbidden.telegramcoursesbot.menu.handler.StatisticsButtonHandler;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.LessonOrchestrationService;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatisticsMenu implements MenuConfigurer {
    private static final String COURSE_ID_PARAM = "courseId";

    private final StatisticsButtonHandler statisticsHandler;

    private final CourseOrchestrationService courseService;

    private final LessonOrchestrationService lessonService;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.STATISTICS);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(1);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.STATISTICS_PAGE_0, p.user()));
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(courseService.getByBot(p.user(), p.bot()).stream()
                .map(c -> (Button)new TransitoryButton(c.getLocalizedTitle(), COURSE_ID_PARAM, c.getId().toString(), 1))
                .toList());

            buttons.add(new TerminalButton(loader.localize(
                Localizations.Button.GENERAL_BOT_STATISTICS, p.user()).getData(), statisticsHandler));
                
            buttons.add(new TerminalButton(loader.localize(
                Localizations.Button.BOT_USERS_STATISTICS, p.user()).getData(), statisticsHandler));
            return buttons;
        });

        final Page page2 = new Page(menu);

        page2.setPageIndex(1);
        page2.setColumns(1);
        page2.setLocalizationFunction(p -> loader.localize(Localizations.Menu.STATISTICS_PAGE_1, p.user()));
        page2.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.COURSE_STATISTICS, p.user()).getData(), statisticsHandler));
            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.COURSE_USERS_STATISTICS, p.user()).getData(), 2));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.user()).getData()));

            return buttons;
        });

        final Page page3 = new Page(menu);

        page3.setPageIndex(2);
        page3.setColumns(2);
        page3.setLocalizationFunction(p -> loader.localize(Localizations.Menu.STATISTICS_PAGE_2, p.user()));
        page3.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            
            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.COURSE_USERS_BY_STAGE, p.user()).getData(), 3));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.COURSE_COMPLETED_USERS, p.user()).getData(), statisticsHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.COURSE_ALL_USERS, p.user()).getData(), statisticsHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.user()).getData()));

            return buttons;
        });

        final Page page4 = new Page(menu);

        page4.setPageIndex(3);
        page4.setColumns(3);
        page4.setLocalizationFunction(p -> {
            final StringBuilder builder = new StringBuilder();
            final List<UsersByCourseStageCountDto> countDtos = courseService
                    .countAndGroupByCourseStage(Long.parseLong(p.params().get(COURSE_ID_PARAM)));

            for (final var dto : countDtos) {
                builder.append(dto.stage()).append(" — ").append(dto.numberOfUsers()).append('\n');
            }
            builder.delete(builder.length() - 1, builder.length());
            
            return loader.localize(Localizations.Menu.STATISTICS_PAGE_3, p.user(),
                    new Localizations.Menu.StatisticsPage3Params(builder.toString()));
        });
        page4.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            
            for (int i = 0; i < lessonService.countByCourse(Long.parseLong(p.params().get(COURSE_ID_PARAM))); ++i) {
                buttons.add(new TerminalButton(String.valueOf(i), String.valueOf(i), statisticsHandler));
            }
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.user()).getData()));

            return buttons;
        });

        menu.setPages(List.of(page1, page2, page3, page4));

        return menu;
    }
}

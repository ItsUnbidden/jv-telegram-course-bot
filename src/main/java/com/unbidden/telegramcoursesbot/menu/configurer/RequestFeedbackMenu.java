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
import com.unbidden.telegramcoursesbot.menu.handler.AcceptHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.DeclineHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.TransferHomeworkButtonHandler;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.UserOrchestrationService;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestFeedbackMenu implements MenuConfigurer {
    private static final String BOT_ROLE_ID_PARAM = "botRoleId";
    private static final String WITH_COMMENT_PARAM = "withComment";

    private final AcceptHomeworkButtonHandler acceptHandler;
    private final DeclineHomeworkButtonHandler declineHandler;
    private final TransferHomeworkButtonHandler transferHomeworkHandler;

    private final LocalizationLoader localizationLoader;

    private final UserOrchestrationService userService;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.REQUEST_FEEDBACK);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(1);
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(localizationLoader.localize(Localizations.Button.DECLINE_HOMEWORK, p.botRole()).getData(), declineHandler));
            buttons.add(new TransitoryButton(localizationLoader.localize(Localizations.Button.GENERAL_ACCEPT_HOMEWORK, p.botRole()).getData(), 1));

            if (userService.countHomeworkReceivers(p.botRole()) > 0) {
                buttons.add(new TransitoryButton(localizationLoader.localize(Localizations.Button.TRANSFER_HOMEWORK, p.botRole()).getData(), 2));
            }

            return buttons;
        });
                
        final Page page2 = new Page(menu);

        page2.setPageIndex(1);
        page2.setColumns(1);
        page2.setButtonsFunction(p -> List.of(
            new TerminalButton(localizationLoader.localize(Localizations.Button.ACCEPT_HOMEWORK, p.botRole()).getData(),
                    WITH_COMMENT_PARAM, String.valueOf(false), acceptHandler),
            new TerminalButton(localizationLoader.localize(Localizations.Button.ACCEPT_HOMEWORK_WITH_COMMENT, p.botRole()).getData(),
                    WITH_COMMENT_PARAM, String.valueOf(true), acceptHandler),
            new BackwardButton(localizationLoader.localize(Localizations.Button.BACK, p.botRole()).getData())
        ));
                
        final Page page3 = new Page(menu);

        page3.setPageIndex(2);
        page3.setColumns(2);
        page3.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            final List<BotRole> botRoles = userService.getHomeworkReceivers(p.botRole());

            buttons.addAll(botRoles.stream()
                    .filter(br -> !br.getId().equals(p.botRole().getId()))
                    .map(br -> new TransitoryButton(br.getUser().getFirstName(), BOT_ROLE_ID_PARAM, br.getId().toString(), 3))
                    .toList());
            buttons.add(new BackwardButton(localizationLoader.localize(Localizations.Button.BACK, p.botRole()).getData()));
            
            return buttons;
        });
                
        final Page page4 = new Page(menu);

        page4.setPageIndex(3);
        page4.setColumns(2);
        page4.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(localizationLoader.localize(Localizations.Button.CONFIRM, p.botRole()).getData(), transferHomeworkHandler));
            buttons.add(new BackwardButton(localizationLoader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        menu.setPages(List.of(page1, page2, page3, page4));
        
        return menu;
    }
}

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
import com.unbidden.telegramcoursesbot.menu.handler.ReplyToSupportRequestButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.TransferSupportRequestButtonHandler;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.UserOrchestrationService;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportReplyMenu implements MenuConfigurer {
    private static final String BOT_ROLE_ID_PARAM = "botRoleId";

    private final ReplyToSupportRequestButtonHandler replyToSupportRequestHandler;
    private final TransferSupportRequestButtonHandler transferSupportRequestHandler;

    private final LocalizationLoader loader;

    private final UserOrchestrationService userService;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.SUPPORT_REPLY);

        final Page page1 = new Page(menu);

        page1.setPageIndex(0);
        page1.setColumns(1);
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.REPLY_TO_SUPPORT_REQUEST, p.botRole()).getData(), replyToSupportRequestHandler));

            if (userService.countOtherSupportReceivers(p.botRole()) > 0) {
                buttons.add(new TransitoryButton(loader.localize(Localizations.Button.TRANSFER_SUPPORT, p.botRole()).getData(), 1));
            }

            return buttons;
        });

        final Page page2 = new Page(menu);

        page2.setPageIndex(1);
        page2.setColumns(2);
        page2.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            final List<BotRole> botRoles = userService.getSupportReceivers(p.botRole());
            
            buttons.addAll(botRoles.stream()
                    .filter(br -> !br.getId().equals(p.botRole().getId()))
                    .map(br -> new TransitoryButton(br.getUser().getFullName(), BOT_ROLE_ID_PARAM, br.getId().toString(), 2))
                    .toList());
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page page3 = new Page(menu);

        page3.setPageIndex(2);
        page3.setColumns(2);
        page3.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.CONFIRM, p.botRole()).getData(), transferSupportRequestHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData())
        ));

        menu.setPages(List.of(page1, page2, page3));

        return menu;
    }
}

package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.ReplyToSupportReplyButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.ResolveSupportRequestButtonHandler;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportReplyToReplyMenu implements MenuConfigurer {
    private static final String REPLY_ID_PARAM = "replyId";

    private final ReplyToSupportReplyButtonHandler replyToSupportReplyHandler;
    private final ResolveSupportRequestButtonHandler resolveRequestHandler;

    private final LocalizationLoader localizationLoader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.SUPPORT_REPLY_TO_REPLY);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(1);
        page.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            if (p.params().containsKey(REPLY_ID_PARAM)) {
                buttons.add(new TerminalButton(localizationLoader.localize(Localizations.Button.REPLY_TO_SUPPORT_REPLY, p.botRole()).getData(), replyToSupportReplyHandler));
            }
            buttons.add(new TerminalButton(localizationLoader.localize(Localizations.Button.RESOLVE_SUPPORT_REQUEST, p.botRole()).getData(), resolveRequestHandler));

            return buttons;
        });

        menu.setPages(List.of(page));
        menu.setOneTimeMenu(false);
        
        return menu;
    }
}

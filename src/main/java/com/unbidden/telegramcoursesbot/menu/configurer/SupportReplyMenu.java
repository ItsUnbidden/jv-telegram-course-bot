package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.ReplyToSupportRequestButtonHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportReplyMenu implements MenuConfigurer {
    private final ReplyToSupportRequestButtonHandler replyToSupportRequestHandler;

    private final LocalizationLoader loader;
    
    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.SUPPORT_REPLY);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(1);
        page.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.REPLY_TO_SUPPORT_REQUEST, p.user()).getData(), replyToSupportRequestHandler)
        ));

        menu.setPages(List.of(page));

        return menu;
    }
}

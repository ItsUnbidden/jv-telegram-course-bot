package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.handler.ReplyToSupportRequestButtonHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportReplyMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_rpl";

    private static final String REPLY_TO_SUPPORT_REQUEST = "rtsr";

    private static final String BUTTON_REPLY_TO_SUPPORT_REQUEST = "button_reply_to_support_request";

    private final ReplyToSupportRequestButtonHandler replyToSupportRequestHandler;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;
    
    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_REPLY_TO_SUPPORT_REQUEST, u)
                .getData(), REPLY_TO_SUPPORT_REQUEST, replyToSupportRequestHandler)));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(true);
        menu.setAttachedToMessage(true);
        menuService.save(menu);
    }
}

package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.support.SupportService;
import com.unbidden.telegramcoursesbot.service.menu.handler.ReplyToSupportReplyButtonHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportReplyToReplyMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_rplToRpl";
    
    private static final String MARK_AS_RESOLVED = "mar";
    private static final String REPLY_TO_SUPPORT_REPLY = "rtsr";

    private static final String BUTTON_REPLY_TO_SUPPORT_REPLY = "button_reply_to_support_reply";
    private static final String BUTTON_RESOLVE_SUPPORT_REQUEST = "button_resolve_support_request";

    private final ReplyToSupportReplyButtonHandler replyToSupportReplyHandler;

    private final MenuService menuService;

    private final SupportService supportService;

    private final LocalizationLoader localizationLoader;
    
    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_REPLY_TO_SUPPORT_REPLY, u)
                .getData(), REPLY_TO_SUPPORT_REPLY, replyToSupportReplyHandler),
                new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_RESOLVE_SUPPORT_REQUEST, u).getData(), MARK_AS_RESOLVED,
                (u1, pa) -> supportService.markAsResolved(u, supportService.getReplyById(
                    Long.parseLong(pa[0]), u).getRequest()))));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setOneTimeMenu(true);
        menu.setInitialParameterPresent(true);
        menu.setAttachedToMessage(true);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

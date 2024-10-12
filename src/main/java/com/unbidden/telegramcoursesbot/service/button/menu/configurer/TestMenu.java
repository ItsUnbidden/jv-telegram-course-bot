package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class TestMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_tst";

    private static final String TEST = "test";

    private static final String BUTTON_TEST_MENU = "button_test_menu";

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_TEST_MENU, u)
                .getData(), TEST, (u1, pa) -> {
                    bot.sendMessage(SendMessage.builder()
                        .chatId(u1.getId())
                        .text("Test button triggered.")
                        .build());
                    final RuntimeException exception = new RuntimeException("Test exception");

                    throw exception;
                })));
        final Page terminalPage = new Page();
        terminalPage.setMenu(menu);
        terminalPage.setPageIndex(1);
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page, terminalPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(true);
        menu.setAttachedToMessage(true);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

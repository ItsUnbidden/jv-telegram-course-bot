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
    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                "menu_test_page_0", u));
        page.setButtonsFunction(u -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser("button_test_menu", u)
                .getData(), "sh", (p, u1) -> {
                    bot.sendMessage(SendMessage.builder()
                            .chatId(u1.getId())
                            .text("Test button triggered.")
                            .build());
                })));
        final Page terminalPage = new Page();
        terminalPage.setMenu(menu);
        terminalPage.setPageIndex(1);
        terminalPage.setLocalizationFunction((u, p) -> localizationLoader.getLocalizationForUser(
                "menu_test_terminal_page", u));
        menu.setName("m_tst");
        menu.setPages(List.of(page, terminalPage));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(true);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}

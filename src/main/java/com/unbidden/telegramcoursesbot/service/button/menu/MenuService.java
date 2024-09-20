package com.unbidden.telegramcoursesbot.service.button.menu;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;

public interface MenuService {
    void initiateMenu(String menuName, User user);

    void processCallbackQuery(CallbackQuery query);
}
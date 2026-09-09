package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import com.unbidden.telegramcoursesbot.menu.NamedSpringBean;
import com.unbidden.telegramcoursesbot.model.BotRole;

public abstract class AbstractButtonHandler extends NamedSpringBean {
    public abstract void handle(BotRole botRole, Map<String, String> params);
}

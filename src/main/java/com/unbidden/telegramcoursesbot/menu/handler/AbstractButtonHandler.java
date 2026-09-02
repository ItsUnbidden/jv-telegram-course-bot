package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.beans.factory.BeanNameAware;

import com.unbidden.telegramcoursesbot.model.BotRole;

public abstract class AbstractButtonHandler implements BeanNameAware {
    private String beanName;

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    public String getBeanName() {
        return beanName;
    }

    public abstract void handle(BotRole botRole, Map<String, String> params);
}

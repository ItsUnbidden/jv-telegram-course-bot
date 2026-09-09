package com.unbidden.telegramcoursesbot.menu;

import org.springframework.beans.factory.BeanNameAware;

public abstract class NamedSpringBean implements BeanNameAware {
    private String beanName;

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    public String getBeanName() {
        return beanName;
    }
}

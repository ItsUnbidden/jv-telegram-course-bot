package com.unbidden.telegramcoursesbot.util;

import lombok.Data;

@Data
public class Tag {
    private String name;

    private boolean isInjectionRequired;

    public Tag(String name, boolean isInjectionRequired) {
        this.name = name;
        this.isInjectionRequired = isInjectionRequired;
    }
}

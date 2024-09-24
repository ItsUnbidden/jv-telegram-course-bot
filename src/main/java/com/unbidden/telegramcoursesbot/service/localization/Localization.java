package com.unbidden.telegramcoursesbot.service.localization;

import lombok.Data;

@Data
public class Localization {
    private String name;

    private String data;

    public Localization(String name, String data) {
        this.name = name;
        this.data = data;
    }

    public Localization(String data) {
        this.data = data;
    }
}

package com.unbidden.telegramcoursesbot.localization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

@Data
@EqualsAndHashCode
public class Localization implements Cloneable {
    private String name;

    private String data;

    private List<MessageEntity> entities;

    private Set<String> params;

    private boolean isInjectionRequired;

    public Localization(String name, String data, Set<String> params, boolean isInjectionRequired) {
        this.name = name;
        this.data = data;
        this.params = params;
        this.isInjectionRequired = isInjectionRequired;
    }

    public Localization(String data) {
        this.data = data;
        this.entities = new ArrayList<>();
        this.isInjectionRequired = false;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

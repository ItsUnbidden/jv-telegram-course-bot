package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Embeddable;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
@Embeddable
public class ContentTextData {
    private String data;

    private boolean isLocalization;

    public ContentTextData() {

    }

    public ContentTextData(@NonNull String data, boolean isLocalization) {
        this.data = data;
        this.isLocalization = isLocalization;
    }
}

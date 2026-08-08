package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("TEXT")
public class LocalizedContent extends Content {
    @Column(nullable = false)
    private String languageCode;

    @Override
    public String toString() {
        return "LocalizedContent(id=" + getId() + ", botId=" + getBot().getId() + ", type=" + getType()
                + ", isProtected=" + isProtected() + ", languageCode=" + languageCode + ")";
    }
}

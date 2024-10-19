package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "content")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class LocalizedContent extends Content {
    private String languageCode;

    public LocalizedContent() {
        super(MediaType.TEXT);
    }

    public LocalizedContent(MediaType type) {
        super(type);
    }
}

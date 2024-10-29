package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public abstract class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private ContentTextData data;

    @Column(nullable = false, insertable = false, updatable = false)
    private MediaType type;

    public enum MediaType {
        TEXT,
        GRAPHICS,
        AUDIO,
        DOCUMENT
    }
}

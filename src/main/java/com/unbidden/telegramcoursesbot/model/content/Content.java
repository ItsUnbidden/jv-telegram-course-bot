package com.unbidden.telegramcoursesbot.model.content;

import com.unbidden.telegramcoursesbot.model.Bot;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public abstract class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

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

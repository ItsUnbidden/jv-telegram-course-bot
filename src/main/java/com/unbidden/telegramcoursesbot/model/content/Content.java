package com.unbidden.telegramcoursesbot.model.content;

import com.unbidden.telegramcoursesbot.model.BaseEntity;
import com.unbidden.telegramcoursesbot.model.Bot;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "content")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class Content extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    private String data;

    @Column(nullable = false, insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private MediaType type;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isProtected;

    @Override
    public String toString() {
        return "Content(id=" + getId() + ", botId=" + bot.getId() + ", type=" + type + ", isProtected=" + isProtected + ")";
    }

    public enum MediaType {
        TEXT,
        GRAPHICS,
        AUDIO,
        DOCUMENT
    }
}

package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.util.List;

import org.hibernate.Hibernate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("AUDIO")
public class AudioContent extends LocalizedContent {
    @ManyToMany()
    @JoinTable(name = "content_audios",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "audio_id"))
    private List<Audio> audios;

    @Override
    public String toString() {
        return "AudioContent(id=" + getId() + ", botId=" + getBot().getId() + ", type=" + getType()
                + ", isProtected=" + isProtected() + ", languageCode=" + getLanguageCode()
                + (Hibernate.isInitialized(audios) ? ", audioIds=" + audios.stream().map(p -> p.getId()).toList() : ", audioIds=LAZY") + ")";
    }
}

package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class AudioContent extends LocalizedContent {
    @ManyToMany()
    @JoinTable(name = "content_audios",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "audio_id"))
    private List<Audio> audios;

    public AudioContent() {
        super(MediaType.AUDIO);
    }
}

package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.lang.NonNull;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Audio extends Document {
    private String title;

    private String performer;

    @Column(nullable = false)
    private Integer duration;

    public Audio() {

    }

    /**
     * Creates an audio entity out of telegram audio object. Warning: thumbnail is not set!
     * @param extAudio — telegram audio
     */
    public Audio(@NonNull org.telegram.telegrambots.meta.api.objects.Audio extAudio) {
        super(extAudio.getFileId(), extAudio.getFileUniqueId(), extAudio.getFileSize(),
                extAudio.getFileName(), extAudio.getMimeType());
        this.setTitle(extAudio.getTitle());
        this.setPerformer(extAudio.getPerformer());
        this.setDuration(extAudio.getDuration());
    }
}

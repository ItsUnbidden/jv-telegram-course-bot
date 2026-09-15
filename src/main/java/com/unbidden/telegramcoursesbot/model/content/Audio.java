package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import org.springframework.lang.NonNull;

@Getter
@Setter
@Entity
@DiscriminatorValue("AUDIO")
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

    @Override
    public String toString() {
        return "Audio(uniqueId=" + getUniqueId() + ", id=" + getId() + ", fileSize=" + getFileSize()
                + ", fileName=" + getFileName() + ", thumbnailUniqueId=" + (getThumbnail() != null ? getThumbnail().getUniqueId() : "NULL")
                + ", mimeType=" + getMimeType() + ", title=" + title + ", performer=" + performer
                + ", duration=" + duration + ")";
    }
}

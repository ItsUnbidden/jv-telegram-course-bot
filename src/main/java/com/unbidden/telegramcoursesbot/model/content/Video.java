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
@DiscriminatorValue("VIDEO")
public class Video extends Document {
    @Column(nullable = false)
    private Integer height;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer duration;

    public Video() {

    }

    public Video(@NonNull org.telegram.telegrambots.meta.api.objects.Video extVideo) {
        super(extVideo.getFileId(), extVideo.getFileUniqueId(), extVideo.getFileSize(),
                extVideo.getFileName(), extVideo.getMimeType());
        this.height = extVideo.getHeight();
        this.width = extVideo.getWidth();
        this.duration = extVideo.getDuration();
    }

    @Override
    public String toString() {
        return "Video(uniqueId=" + getUniqueId() + ", id=" + getId() + ", fileSize=" + getFileSize()
                + ", fileName=" + getFileName() + ", thumbnailUniqueId=" + (getThumbnail() != null ? getThumbnail().getUniqueId() : "NULL")
                + ", mimeType=" + getMimeType() + ", height=" + height + ", width=" + width
                + ", duration=" + duration + ")";
    }
}

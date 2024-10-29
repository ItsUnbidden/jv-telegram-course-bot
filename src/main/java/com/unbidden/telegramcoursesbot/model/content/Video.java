package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.lang.NonNull;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
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
}

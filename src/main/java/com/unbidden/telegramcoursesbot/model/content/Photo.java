package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

@Getter
@Setter
@Entity
@Table(name = "photos")
public class Photo extends File {    
    @Column(nullable = false)
    private Integer height;

    @Column(nullable = false)
    private Integer width;

    private String filePath;

    public Photo() {

    }

    public Photo(@NonNull PhotoSize extPhoto) {
        super(extPhoto.getFileId(), extPhoto.getFileUniqueId(), (long)extPhoto.getFileSize());
        this.height = extPhoto.getHeight();
        this.width = extPhoto.getWidth();
        this.filePath = extPhoto.getFilePath();
    }

    @Override
    public String toString() {
        return "Photo(uniqueId=" + getUniqueId() + ", id=" + getId() + ", fileSize=" + getFileSize()
                + ", height=" + height + ", width=" + width + ", filePath=" + filePath + ")";
    }
}

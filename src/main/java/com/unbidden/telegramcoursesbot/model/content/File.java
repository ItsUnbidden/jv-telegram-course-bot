package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
@MappedSuperclass
public abstract class File {
    @Id
    private String uniqueId;

    @Column(nullable = false)
    private String id;

    private Long fileSize;

    public File() {
        
    }

    public File(@NonNull String id, @NonNull String uniqueId, Long fileSize) {
        this.id = id;
        this.uniqueId = uniqueId;
        this.fileSize = fileSize;
    }
}

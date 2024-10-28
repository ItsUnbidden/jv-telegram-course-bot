package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.lang.NonNull;

@Data
@Entity
@Table(name = "documents")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@EqualsAndHashCode(callSuper = true)
@DiscriminatorColumn(name = "document_type")
public class Document extends File {
    private String fileName;

    @OneToOne
    @JoinColumn(name = "thumbnail_photo_id")
    private Photo thumbnail;

    private String mimeType;

    public Document() {

    }

    public Document(@NonNull String id, @NonNull String uniqueId, Long fileSize,
            String fileName, String mimeType) {
        super(id, uniqueId, fileSize);
        this.fileName = fileName;
        this.mimeType = mimeType;
    }

    /**
     * Creates a document entity out of telegram document object. Warning: thumbnail is not set!
     * @param extDocument — telegram document
     */
    public Document(@NonNull org.telegram.telegrambots.meta.api.objects.Document extDocument) {
        super(extDocument.getFileId(), extDocument.getFileUniqueId(), extDocument.getFileSize());
        this.fileName = extDocument.getFileName();
        this.mimeType = extDocument.getMimeType();
    }
}

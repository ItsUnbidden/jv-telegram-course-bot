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
public class DocumentContent extends LocalizedContent {
    @ManyToMany()
    @JoinTable(name = "content_documents",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id"))
    private List<Document> documents;

    public DocumentContent() {
        super(MediaType.DOCUMENT);
    }
}

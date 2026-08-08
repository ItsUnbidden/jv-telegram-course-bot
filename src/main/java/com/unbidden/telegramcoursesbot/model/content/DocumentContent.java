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
@DiscriminatorValue("DOCUMENT")
public class DocumentContent extends LocalizedContent {
    @ManyToMany()
    @JoinTable(name = "content_documents",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id"))
    private List<Document> documents;

    @Override
    public String toString() {
        return "DocumentContent(id=" + getId() + ", botId=" + getBot().getId() + ", type=" + getType()
                + ", isProtected=" + isProtected() + ", languageCode=" + getLanguageCode()
                + (Hibernate.isInitialized(documents) ? ", documentIds=" + documents.stream().map(p -> p.getId()).toList() : ", documentIds=LAZY") + ")";
    }
}

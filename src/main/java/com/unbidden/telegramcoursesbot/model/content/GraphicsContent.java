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
@DiscriminatorValue("GRAPHICS")
public class GraphicsContent extends LocalizedContent {
    @ManyToMany()
    @JoinTable(name = "content_videos",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "video_id"))
    private List<Video> videos;

    @ManyToMany()
    @JoinTable(name = "content_photos",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "photo_id"))
    private List<Photo> photos;

    @Override
    public String toString() {
        return "GraphicsContent(id=" + getId() + ", botId=" + getBot().getId() + ", type=" + getType()
                + ", isProtected=" + isProtected() + ", languageCode=" + getLanguageCode()
                + (Hibernate.isInitialized(photos) ? ", photoIds=" + photos.stream().map(p -> p.getId()).toList() : ", photoIds=LAZY")
                + (Hibernate.isInitialized(videos) ? ", videoIds=" + videos.stream().map(v -> v.getId()).toList() : ", videoIds=LAZY") + ")";
    }
}

package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.DiscriminatorValue;
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
@DiscriminatorValue("1")
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
}

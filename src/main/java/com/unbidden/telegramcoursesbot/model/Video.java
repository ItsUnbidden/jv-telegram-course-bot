package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "videos")
public class Video {
    @Id
    private String uniqueId;

    @Column(nullable = false)
    private String id;
    
    private String name;
    
    @Column(nullable = false)
    private Integer duration;
    
    private Long size;
    
    @Column(nullable = false)
    private Integer height;

    @Column(nullable = false)
    private Integer width;
    
    private String mimeType;
}

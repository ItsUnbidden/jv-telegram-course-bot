package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "photos")
public class Photo {
    @Id
    private String uniqueId;

    @Column(nullable = false)
    private String id;
    
    private Integer size;
    
    private Integer height;

    private Integer width;
}

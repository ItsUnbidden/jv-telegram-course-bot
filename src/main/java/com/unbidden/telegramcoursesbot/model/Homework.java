package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "homework")
public class Homework {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_mapping_id", nullable = false)
    private ContentMapping mapping;

    // Should be like: TEXT GRAPHICS
    private String allowedMediaTypes;

    @OneToOne(mappedBy = "homework")
    private Lesson lesson;

    private boolean isFeedbackRequired;

    private boolean isRepeatedCompletionAvailable;
}

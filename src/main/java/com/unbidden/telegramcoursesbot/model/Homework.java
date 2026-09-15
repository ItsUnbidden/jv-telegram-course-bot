package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "homework")
public class Homework extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "content_mapping_id", nullable = false)
    private ContentMapping mapping;

    // Should be like: TEXT GRAPHICS
    private String allowedMediaTypes;

    @OneToOne(mappedBy = "homework")
    private Lesson lesson;

    /**
     * Delay before this homework will be sent to user. Specified in minutes.
     * If less then 0 then it is interpreted as no delay.
     */
    @Column(nullable = false)
    private Integer delay;

    private boolean isFeedbackRequired;

    private boolean isRepeatedCompletionAvailable;

    @Version
    private Long version;

    @Override
    public String toString() {
        return "Homework(id=" + getId() + ", mappingId=" + mapping.getId() + ", allowedMediaTypes=\"" + allowedMediaTypes + "\", lessonId=" + lesson.getId()
                + ", delay=" + delay + ", isFeedbackRequired=" + isFeedbackRequired + ", isRepeatedCompletionAvailable=" + isRepeatedCompletionAvailable
                + ", version=" + version + ")";
    }
}

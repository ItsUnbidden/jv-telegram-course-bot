package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.List;

import org.hibernate.Hibernate;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "courses")
public class Course extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "title_mapping_id", nullable = false)
    private ContentMapping title;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "end_mapping_id")
    private ContentMapping endMapping;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "course", cascade = CascadeType.REMOVE)
    private List<Lesson> lessons;

    @Embedded
    private CourseInvoice invoice;

    @Column(nullable = false)
    private boolean isUnderMaintenance;

    @Column(nullable = false)
    private boolean isHomeworkIncluded;

    @Column(nullable = false)
    private boolean isFeedbackIncluded;

    @Version
    private Long version;

    @Override
    public String toString() {
        return "Course(id=" + getId() + ", titleMappingId=" + title.getId()
                + ", endMappingId=" + (endMapping != null ? endMapping.getId() : "NULL")
                + ", botId=" + bot.getId() + ", lessonIds=" + (Hibernate.isInitialized(lessons) ? lessons.stream().map(l -> l.getId()).toList() : "LAZY") 
                + ", invoice=" + invoice + ", isUnderMaintenance=" + isUnderMaintenance + ", isHomeworkIncluded=" + isHomeworkIncluded
                + ", isFeedbackIncluded=" + isFeedbackIncluded + ", version=" + version + ")";
    }
}

package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.List;

import org.hibernate.Hibernate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lessons")
public class Lesson extends BaseEntity implements Comparable<Lesson> {
    @Column(nullable = false)
    private Integer position;

    @OneToMany
    @OrderBy("position ASC")
    @JoinTable(name = "lessons_content_mappings",
            joinColumns = @JoinColumn(name = "lesson_id"),
            inverseJoinColumns = @JoinColumn(name = "mapping_id"))
    private List<ContentMapping> structure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "homework_id")
    private Homework homework;

    /**
     * Delay before this lesson will be sent to user. Specified in minutes.
     * If less then 0 then it is interpreted as no delay.
     */
    @Column(nullable = false)
    private Integer delay;

    @Version
    private Long version;

    public boolean isHomeworkIncluded() {
        return homework != null;
    }

    @Override
    public String toString() {
        return "Lesson(id=" + getId() + ", position=" + position
                + ", structure=" + (Hibernate.isInitialized(structure) ? structure.stream().map(m -> m.getId()).toList() : "LAZY")
                + ", courseId=" + course.getId() + ", homeworkId=" + (homework != null ? homework.getId() : "NULL")
                + ", delay=" + delay + ", version=" + version + ")";
    }

    @Override
    public int compareTo(Lesson o) {
        if (this.position > o.getPosition()) {
            return 1;
        }
        if (this.position < o.getPosition()) {
            return -1;
        }
        return 0;
    }
}

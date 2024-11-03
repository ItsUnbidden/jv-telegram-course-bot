package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;

@Entity
@Data
@Table(name = "lessons")
public class Lesson implements Comparable<Lesson> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer position;

    @OneToMany()
    @JoinTable(name = "lessons_content_mappings",
            joinColumns = @JoinColumn(name = "lesson_id"),
            inverseJoinColumns = @JoinColumn(name = "mapping_id"))
    private List<ContentMapping> structure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id")
    private Homework homework;

    @Column(nullable = false)
    private SequenceOption sequenceOption;

    public boolean isHomeworkIncluded() {
        return homework != null;
    }
    
    public enum SequenceOption {
        TIMED,
        BUTTON,
        HOMEWORK
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

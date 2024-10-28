package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.lang.NonNull;

@Entity
@Data
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime basicSubmittedTimestamp;

    private LocalDateTime advancedSubmittedTimestamp;

    private LocalDateTime lastUpdateTimestamp;

    @Column(nullable = false)
    private Integer originalCourseGrade;

    @Column(nullable = false)
    private Integer originalPlatformGrade;

    @Column(nullable = false)
    private Integer courseGrade;

    @Column(nullable = false)
    private Integer platformGrade;

    @OneToOne
    @JoinColumn(name = "original_content_id")
    private LocalizedContent originalContent;

    @OneToOne
    @JoinColumn(name = "content_id")
    private LocalizedContent content;

    @ManyToMany
    @JoinTable(name = "reviews_users_who_read", joinColumns = @JoinColumn(name = "review_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<UserEntity> markedAsReadBy;

    @OneToOne
    @JoinColumn(name = "comment_content_id")
    private LocalizedContent commentContent;

    @ManyToOne
    @JoinColumn(name = "user_who_commented_id")
    private UserEntity commentedBy;

    private LocalDateTime commentedAt;

    @NonNull
    public String getUsersWhoReadAsString() {
        final StringBuilder builder = new StringBuilder();

        for (UserEntity user : markedAsReadBy) {
            builder.append(user.getFullName()).append(", ");
        }
        if (builder.length() != 0) {
            return builder.delete(builder.length() - 2, builder.length()).toString();
        }
        return "";
    }
}

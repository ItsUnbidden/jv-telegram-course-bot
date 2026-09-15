package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.Hibernate;
import org.springframework.lang.NonNull;

@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime basicSubmittedTimestamp;

    private LocalDateTime advancedSubmittedTimestamp;

    private LocalDateTime lastUpdateTimestamp;

    @Column(nullable = false)
    private Integer originalCourseGrade;

    @Column(nullable = false)
    private Integer courseGrade;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_content_id")
    private LocalizedContent originalContent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private LocalizedContent content;

    @ManyToMany
    @JoinTable(name = "reviews_users_who_read", joinColumns = @JoinColumn(name = "review_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<UserEntity> markedAsReadBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_content_id")
    private LocalizedContent commentContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_who_commented_id")
    private UserEntity commentedBy;

    private LocalDateTime commentedAt;

    @Version
    private Long version;

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

    @Override
    public String toString() {
        return "Review(id=" + getId() + ", userId=" + user.getId() + ", courseId=" + course.getId() + ", basicSubmittedTimestamp="
                + basicSubmittedTimestamp + ", advancedSubmittedTimestamp=" + advancedSubmittedTimestamp
                + ", lastUpdateTimestamp=" + lastUpdateTimestamp + ", courseGrade=" + courseGrade
                + ", contentId=" + (content != null ? content.getId() : "NULL")
                + ", markedAsReadBy=" + (Hibernate.isInitialized(markedAsReadBy) ? markedAsReadBy.stream().map(u -> u.getId()) : "LAZY")
                + ", commentContentId=" + (commentContent != null ? commentContent.getId() : "NULL") + ", commentedById="
                + (commentedBy != null ? commentedBy.getId() : "NULL") + ", commentedAt=" + commentedAt + ", version=" + version + ")";
    }
}

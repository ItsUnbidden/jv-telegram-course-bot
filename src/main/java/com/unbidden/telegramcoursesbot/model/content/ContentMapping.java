package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;

import org.hibernate.Hibernate;

import com.unbidden.telegramcoursesbot.model.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "content_mappings")
public class ContentMapping extends BaseEntity implements Comparable<ContentMapping> {
    @Column(nullable = false)
    private Integer position;

    @OneToMany
    @JoinTable(name = "content_mappings_content",
            joinColumns = @JoinColumn(name = "mapping_id"),
            inverseJoinColumns = @JoinColumn(name = "content_id"))
    private List<LocalizedContent> content;

    @Override
    public int compareTo(ContentMapping o) {
        if (this.position > o.getPosition()) {
            return 1;
        }
        if (this.position < o.getPosition()) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "ContentMapping(id=" + getId() + ", position=" + position
                + (Hibernate.isInitialized(content) ? ", content=" + content.stream().map(c -> c.getId()).toList() : ", content=LAZY") + ")";
    }
}

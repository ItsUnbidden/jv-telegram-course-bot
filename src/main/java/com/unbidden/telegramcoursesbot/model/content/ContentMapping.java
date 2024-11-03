package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;

@Entity
@Data
@Table(name = "content_mappings")
public class ContentMapping implements Comparable<ContentMapping> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}

package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;

@Getter
@Setter
@Entity
@Table(name = "bots")
@SQLDelete(sql = "UPDATE bots SET is_disabled = true WHERE id = ?")
@SQLRestriction("is_disabled = false")
public class Bot extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_info_mapping_id")
    private ContentMapping creatorInfo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_mapping_id")
    private ContentMapping terms;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_mapping_id")
    private ContentMapping start;

    private boolean isDisabled;

    @Override
    public String toString() {
        return "Bot(id=" + getId() + ", isDisabled=" + isDisabled + ")";
    }
}

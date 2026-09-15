package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;

@Getter
@Setter
@Entity
@Table(name = "bots")
@SQLDelete(sql = "UPDATE bots SET is_disabled = true WHERE id = ?")
@SQLRestriction("is_disabled = false")
public class Bot extends BaseEntity {
    private static final String ELEMENT_DIVIDER = ":";

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

    @Column(nullable = false)
    private String languages;

    private boolean isDisabled;

    @Override
    public String toString() {
        return "Bot(id=" + getId() + ", creatorInfoMappingId=" + (creatorInfo != null ? creatorInfo.getId() : "NULL")
                + ", termsMappingId=" + (terms != null ? terms.getId() : "NULL")
                + ", startMappingId=" + (start != null ? start.getId() : "NULL")
                + ", isDisabled=" + isDisabled + ")";
    }

    @Transient
    public List<String> languagesToList() {
        return List.of(this.languages.split(ELEMENT_DIVIDER));
    }

    @Transient
    public void parseAndSetLanguages(List<String> languagesList) {
        Assert.notEmpty(languagesList, "languagesList cannot be empty or null.");
        Assert.noNullElements(languagesList, "languagesList cannot contain null.");

        final StringBuilder builder = new StringBuilder();

        for (final String code : languagesList) {
            builder.append(code).append(ELEMENT_DIVIDER);
        }
        if (builder.length() > 0) {
            builder.delete(builder.length() - 1, builder.length());
        }
        
        this.languages = builder.toString();
    }
}

package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "content")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@EqualsAndHashCode(callSuper = true)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.INTEGER)
@DiscriminatorValue("0")
public class LocalizedContent extends Content {
    private String languageCode;
}

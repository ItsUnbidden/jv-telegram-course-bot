package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import com.unbidden.telegramcoursesbot.model.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "markers")
public class MarkerArea extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Integer offset;

    @Column(nullable = false)
    private Integer length;

    private String url;

    private String language;

    private String customEmojiId;

    private String text;

    public MarkerArea() {

    }

    /**
     * Maps Telegram {@link MessageEntity} to this class.
     * @param messageEntity
     */
    public MarkerArea(@NonNull MessageEntity messageEntity, @NonNull Content content) {
        this.content = content;
        this.type = messageEntity.getType();
        this.offset = messageEntity.getOffset();
        this.length = messageEntity.getLength();
        this.url = messageEntity.getUrl();
        this.language = messageEntity.getLanguage();
        this.customEmojiId = messageEntity.getCustomEmojiId();
        this.text = messageEntity.getText();
    }

    @NonNull
    public MessageEntity toMessageEntity() {
        final MessageEntity entity = new MessageEntity(type, offset, length);
        entity.setUrl(url);
        entity.setLanguage(language);
        entity.setCustomEmojiId(customEmojiId);
        entity.setText(text);
        return entity;
    }

    @Override
    public String toString() {
        return "MarkerArea(id=" + getId() + ", contentId=" + content.getId() + ", type=" + type + ")";
    }
}

package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

@Entity
@Table(name = "markers")
@Data
public class MarkerArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    public MarkerArea(@NonNull MessageEntity messageEntity) {
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
}

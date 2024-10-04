package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

}

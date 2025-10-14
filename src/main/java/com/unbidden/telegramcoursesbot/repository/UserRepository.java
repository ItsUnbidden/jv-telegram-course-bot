package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

}

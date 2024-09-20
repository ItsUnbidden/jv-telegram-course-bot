package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    
}

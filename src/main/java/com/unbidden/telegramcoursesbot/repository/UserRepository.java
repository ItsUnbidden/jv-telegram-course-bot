package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Query("from UserEntity u where u.isAdmin = true")
    @NonNull
    List<UserEntity> findAllAdmins();

    @Query("from UserEntity u where u.isAdmin = true and u.isReceivingHomeworkRequests = true")
    @NonNull
    List<UserEntity> findAllHomeworkReceivingAdmins();
}

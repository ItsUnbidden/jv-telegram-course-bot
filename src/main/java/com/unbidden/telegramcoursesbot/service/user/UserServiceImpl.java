package com.unbidden.telegramcoursesbot.service.user;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = LogManager.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    @Value("${telegram.bot.authorization.default.admin.id}")
    private Long defaultAdminId;

    @Override
    @Nullable
    public UserEntity addAdmin(@NonNull Long userId) {
        Optional<UserEntity> potentialUser = userRepository
                .findById(userId);
        if (potentialUser.isPresent()) {
            final UserEntity userFromDb = potentialUser.get();

            LOGGER.info("Adding user " + userFromDb.getId() + " to the admin list...");
            if (userFromDb.isAdmin()) {
                LOGGER.warn("User " + userFromDb.getId()
                        + " is already an admin. Action ignored.");
                return null;
            }
            userFromDb.setAdmin(true);
            return userRepository.save(userFromDb);
        } else {
            LOGGER.warn("User " + userId + " is inaccessable. This may be because they "
                    + "have never called /start on the bot.");
        }
        return null;
    }

    @Override
    @Nullable
    public UserEntity removeAdmin(@NonNull Long userId) {
        if (userId.longValue() == defaultAdminId.longValue()) {
            LOGGER.warn("Default admin cannot be removed. Action skipped.");
            return null;
        }
        Optional<UserEntity> potentialUser = userRepository
                .findById(userId);
        if (potentialUser.isPresent()) {
            final UserEntity userFromDb = potentialUser.get();

            LOGGER.info("Removing user " + userFromDb.getId() + " from the admin list...");
            if (!userFromDb.isAdmin()) {
                LOGGER.warn("User " + userFromDb.getId() + " is not an admin. Action ignored.");
                return null;
            }
            userFromDb.setAdmin(false);
            return userRepository.save(userFromDb);
        } else {
            LOGGER.warn("User " + userId + " is inaccessable. This may be because they "
                    + "have never called /start on the bot.");
        }
        return null;
    }

    @Override
    public boolean isAdmin(@NonNull UserEntity user) {
        return user.isAdmin();
    }

    @Override
    public boolean isAdmin(@NonNull User user) {
        Optional<UserEntity> potentialUser = userRepository.findById(user.getId());
        if (potentialUser.isEmpty()) {
            LOGGER.warn("User " + user.getId() + " is not recorded in the db, "
                    + "but isAdmin(User user) was called for them."); 
            return false;
        }
        return potentialUser.get().isAdmin();
    }

    @Override
    @NonNull
    public List<UserEntity> getAdminList() {
        return userRepository.findAllAdmins();
    }

    @Override
    @NonNull
    public Long getDefaultAdminId() {
        return defaultAdminId;
    }

    @Override
    @NonNull
    public UserEntity getUser(@NonNull Long id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User "
                + id + " is not registred in the database."));
    }
}

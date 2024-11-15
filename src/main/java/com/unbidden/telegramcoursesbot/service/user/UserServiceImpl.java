package com.unbidden.telegramcoursesbot.service.user;

import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
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

    private static final String ERROR_USER_NOT_FOUND = "error_user_not_found";
    
    private static final String LANGUAGE_PRIORITY_DIVIDER = ",";

    private final UserRepository userRepository;

    private final LocalizationLoader localizationLoader;

    @Value("${telegram.bot.authorization.default.admin.id}")
    private Long defaultAdminId;

    @Value("${telegram.bot.authorization.default.creator.id}")
    private Long creatorId;

    @Value("${telegram.bot.message.language.priority}")
    private String languagePriorityStr;

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
    public List<UserEntity> getHomeworkReveivingAdmins() {
        return userRepository.findAllHomeworkReceivingAdmins();
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
                + id + " is not registred in the database", localizationLoader
                .getLocalizationForUser(ERROR_USER_NOT_FOUND, getDiretor())));
    }

    /**
     * Creates or updates user entity if anything changed. Returns the user.
     */
    @Override
    @NonNull
    public UserEntity updateUser(@NonNull User user) {
        LOGGER.trace("Checking if user " + user.getId() + "' data is up to date...");
        final UserEntity userFromDb = userRepository.findById(user.getId())
                .orElse(new UserEntity(user.getId()));

        boolean hasChanged = false;
        if (!userFromDb.isAdmin() && (user.getId().equals(defaultAdminId)
                || user.getId().equals(creatorId))) {
            userFromDb.setAdmin(true);
            hasChanged = true;
            userFromDb.setReceivingHomeworkRequests(true);
            LOGGER.trace("User " + user.getId() + " is the default admin or creator. Setting...");
        }
        if (user.getIsBot() != userFromDb.isBot()) {
            userFromDb.setBot(user.getIsBot());
            hasChanged = true;
            LOGGER.trace("User is bot. Setting...");
        }
        if (!user.getFirstName().equals(userFromDb.getFirstName())) {
            userFromDb.setFirstName(user.getFirstName());
            hasChanged = true;
            LOGGER.trace("First name is " + user.getFirstName() + ". Setting...");
        }
        if (user.getLanguageCode() != null) {
            if (!user.getLanguageCode().equals(userFromDb.getLanguageCode())) {
                userFromDb.setLanguageCode(user.getLanguageCode());
                hasChanged = true;
                LOGGER.trace("Language code is " + user.getLanguageCode() + ". Setting...");
            }
        } else {
            final String theMostPreferedLanguage = languagePriorityStr
                    .split(LANGUAGE_PRIORITY_DIVIDER)[0].trim();
            userFromDb.setLanguageCode(theMostPreferedLanguage);
            hasChanged = true;
            LOGGER.trace("Language code is unavailable. Setting to "
                    + theMostPreferedLanguage + "...");
        }
        if (user.getLastName() != null && !user.getLastName()
                .equals(userFromDb.getLastName())) {
            userFromDb.setLastName(user.getLastName());
            hasChanged = true;
            LOGGER.trace("Last name is " + user.getLastName() + ". Setting...");
        }
        if (user.getUserName() != null && !user.getUserName()
                .equals(userFromDb.getUsername())) {
            userFromDb.setUsername(user.getUserName());
            hasChanged = true;
            LOGGER.trace("Username is " + user.getUserName() + ". Setting...");
        }
        if (hasChanged) {
            LOGGER.trace("Stuff has changed for user " + user.getId() + ". Persisting...");
            userRepository.save(userFromDb);
            LOGGER.trace("Persist is successful.");
        } else {
            LOGGER.trace("User data is up to date with telegram servers.");
        }
        return userFromDb;
    }

    @Override
    @NonNull
    public UserEntity toogleReceiveHomework(@NonNull UserEntity user) {
        user.setReceivingHomeworkRequests(!user.isReceivingHomeworkRequests());
        return userRepository.save(user);
    }

    @Override
    @NonNull
    public UserEntity getDiretor() {
        // TODO: this is a temporary solution.
        return getUser(defaultAdminId);
    }

    @Override
    @NonNull
    public UserEntity getCreator() {
        // TODO: this is a temporary solution.
        return getUser(creatorId);
    }

    @Override
    @NonNull
    public List<UserEntity> getSupport() {
        // TODO: this is a dummy
        return List.of();
    }

    @Override
    @NonNull
    public List<UserEntity> getMentors() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMentors'");
    }
}

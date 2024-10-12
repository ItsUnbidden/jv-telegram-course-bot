package com.unbidden.telegramcoursesbot.service.user;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.User;

public interface UserService {
    @Nullable
    UserEntity addAdmin(@NonNull Long userId);

    @Nullable
    UserEntity removeAdmin(@NonNull Long userId);

    boolean isAdmin(@NonNull UserEntity user);

    boolean isAdmin(@NonNull User user);

    @NonNull
    List<UserEntity> getAdminList();

    @NonNull
    List<UserEntity> getHomeworkReveivingAdmins();

    @NonNull
    Long getDefaultAdminId();

    @NonNull
    UserEntity getUser(@NonNull Long id);

    @NonNull
    UserEntity updateUser(@NonNull User user);

    @NonNull
    UserEntity toogleReceiveHomework(@NonNull UserEntity user);

    // TODO: implement advanced security
    /**
     * The Director has total control of the application.
     * @return the Director
     */
    @NonNull
    UserEntity getDiretor();

    /**
     * The Creator has control of all creative resourses, but lacks technical access.
     * @return the Creator
     */
    @NonNull
    UserEntity getCreator();

    /**
     * Support staff have the authority to handle support requests.
     * @return list of support staff
     */
    @NonNull
    List<UserEntity> getSupport();

    /**
     * Mentors have the authority to handle homeworks and content related messages.
     * @return list of mentors
     */
    @NonNull
    List<UserEntity> getMentors();
}

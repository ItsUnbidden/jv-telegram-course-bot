package com.unbidden.telegramcoursesbot.service.user;

import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.User;

public interface UserService {
    @NonNull
    UserEntity initializeUserForBot(@NonNull User user, @NonNull Bot bot);

    @NonNull
    UserEntity createDummyDirector();

    @NonNull
    BotRole setRole(@NonNull UserEntity user, @NonNull UserEntity target,
            @NonNull Bot bot, @NonNull Role role);

    @NonNull
    BotRole banUserInBot(@NonNull UserEntity user, @NonNull UserEntity target,
            @NonNull Bot bot, int hours);

    @NonNull
    BotRole liftBanInBot(@Nullable UserEntity user, @NonNull UserEntity target, @NonNull Bot bot);

    @NonNull
    UserEntity banUserGenerally(@NonNull UserEntity user, @NonNull UserEntity target, int hours);

    @NonNull
    UserEntity liftGeneralBan(@Nullable UserEntity user, @NonNull UserEntity target);

    @NonNull
    UserEntity getUser(@NonNull Long id, @NonNull UserEntity user);

    @NonNull
    Role getRole(@NonNull RoleType type);

    @NonNull
    BotRole getBotRole(@NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    UserEntity updateUser(@NonNull User user);

    @NonNull
    List<BotRole> getHomeworkBotRoles(@NonNull Bot bot);

    @NonNull
    UserEntity changeLanguage(@NonNull UserEntity user, @NonNull String newCode);

    @NonNull
    UserEntity resetLanguageToDefault(@NonNull UserEntity user);

    @NonNull
    String getLocalizedTitle(@NonNull UserEntity user, @NonNull UserEntity localizedFor,
            @NonNull Bot bot);

    @NonNull
    BotRole toggleReceiveHomework(@NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    List<Authority> parseAuthorities(@NonNull AuthorityType[] types);

    /**
     * The Director has total control of the application.
     * @return the Director
     */
    @NonNull
    UserEntity getDiretor();

    /**
     * The Creator has control of all bot-specific creative resourses.
     * @return the Creator
     */
    @NonNull
    UserEntity getCreator(@NonNull Bot bot);

    /**
     * Support staff have the authority to handle bot-specific support requests.
     * @return list of support staff
     */
    @NonNull
    List<UserEntity> getSupport(@NonNull Bot bot);

    /**
     * Mentors have the authority to handle bot-specific homeworks and content related messages.
     * @return list of mentors
     */
    @NonNull
    List<UserEntity> getMentors(@NonNull Bot bot);
}

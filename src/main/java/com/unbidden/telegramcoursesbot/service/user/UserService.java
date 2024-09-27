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
    Long getDefaultAdminId();

    @NonNull
    UserEntity getUser(@NonNull Long id);
}

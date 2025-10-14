package com.unbidden.telegramcoursesbot.service.post;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import java.util.List;
import org.springframework.lang.NonNull;

public interface PostService {
    void sendMessages(@NonNull UserEntity sender, @NonNull Bot bot,
            @NonNull List<Role> roles, @NonNull Content content);

    void sendMessagesThroughoutBots(@NonNull UserEntity director, @NonNull List<Role> roles,
            @NonNull Content content);

    void sendPrivateMessageToUser(@NonNull UserEntity user, @NonNull UserEntity target,
            @NonNull Bot bot, @NonNull Content content);

    void checkUserIsInBot(@NonNull UserEntity user, @NonNull UserEntity target, @NonNull Bot bot);

    void checkExecution(@NonNull UserEntity user);

    void checkRoles(@NonNull List<Role> roles, @NonNull UserEntity user);
}

package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import java.util.List;
import org.springframework.lang.NonNull;


public interface SecurityService {
    boolean grantAccess(@NonNull Bot bot, @NonNull UserEntity user,
            @NonNull List<Authority> authorities);

    boolean grantAccess(@NonNull Bot bot, @NonNull UserEntity user,
            @NonNull AuthorityType... authorityTypes);
}

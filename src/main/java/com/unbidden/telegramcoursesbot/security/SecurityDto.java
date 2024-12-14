package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import lombok.Data;

@Data
public class SecurityDto {
    private Bot bot;

    private UserEntity user;

    private List<Authority> authorities;

    public SecurityDto(Bot bot, UserEntity user, List<Authority> authorities) {
        this.bot = bot;
        this.user = user;
        this.authorities = authorities;
    }
}

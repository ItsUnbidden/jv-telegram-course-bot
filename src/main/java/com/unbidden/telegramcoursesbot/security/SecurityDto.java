package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;

import java.util.List;

import lombok.Data;

@Data
public class SecurityDto {
    private BotRole botRole;

    private List<AuthorityType> authorities;

    private boolean isBotLordOnly;

    public SecurityDto(BotRole botRole, List<AuthorityType> authorities, boolean isBotLordOnly) {
        this.botRole = botRole;
        this.authorities = authorities;
        this.isBotLordOnly = isBotLordOnly;
    }
}

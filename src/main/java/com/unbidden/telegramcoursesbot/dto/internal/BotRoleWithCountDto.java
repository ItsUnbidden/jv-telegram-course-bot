package com.unbidden.telegramcoursesbot.dto.internal;

import com.unbidden.telegramcoursesbot.model.BotRole;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BotRoleWithCountDto {
    private final BotRole curator;
    
    private long count;

    public BotRoleWithCountDto(BotRole curator, long count) {
        this.curator = curator;
        this.count = count;
    }
}

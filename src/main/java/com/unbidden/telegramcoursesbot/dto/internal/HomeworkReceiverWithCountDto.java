package com.unbidden.telegramcoursesbot.dto.internal;

import com.unbidden.telegramcoursesbot.model.BotRole;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeworkReceiverWithCountDto {
    private final BotRole curator;
    
    private long numberOfAssignees;

    public HomeworkReceiverWithCountDto(BotRole curator, long numberOfAssignees) {
        this.curator = curator;
        this.numberOfAssignees = numberOfAssignees;
    }
}

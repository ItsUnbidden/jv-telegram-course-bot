package com.unbidden.telegramcoursesbot.dto.internal;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.exception.TelegramException;

import lombok.Data;

@Data
public class SendMessageResultDto {
    private final Message message;

    private final TelegramException exception;

    private final Result result;

    public SendMessageResultDto() {
        this.message = null;
        this.exception = null;
        this.result = Result.SKIPPED;
    }

    public SendMessageResultDto(Message message) {
        this.message = message;
        this.exception = null;
        this.result = Result.OK;
    }

    public SendMessageResultDto(TelegramException exception) {
        this.message = null;
        this.exception = exception;
        this.result = Result.FAILURE;
    }

    public static enum Result {
        OK,
        SKIPPED,
        FAILURE
    }
}

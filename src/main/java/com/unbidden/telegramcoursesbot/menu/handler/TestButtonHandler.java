package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendRichMessage;
import org.telegram.telegrambots.meta.api.objects.richblock.InputRichBlockTable;
import org.telegram.telegrambots.meta.api.objects.richblock.RichBlockTableCell;
import org.telegram.telegrambots.meta.api.objects.richtext.InputRichMessage;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextPlain;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TestButtonHandler extends AbstractButtonHandler {
    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(BotRole botRole, Map<String, String> params) {
        final List<List<RichBlockTableCell>> rows = new ArrayList<>();
        final List<RichBlockTableCell> headers = new ArrayList<>();

        headers.add(RichBlockTableCell.builder().isHeader(true).align("center").valign("middle").text(new RichTextPlain("A")).build());
        headers.add(RichBlockTableCell.builder().isHeader(true).align("center").valign("middle").text(new RichTextPlain("B")).build());
        headers.add(RichBlockTableCell.builder().isHeader(true).align("center").valign("middle").text(new RichTextPlain("C")).build());

        rows.add(headers);
        try {
            clientManager.getClient(botRole.getBot()).execute(SendRichMessage.builder()
                .chatId(botRole.getUser().getId())
                .richMessage(InputRichMessage.builder()
                    .blocks(
                        List.of(InputRichBlockTable.builder()
                            .cells(rows)
                            .build()
                        )
                    )
                    .build()
                )
                .build()
            );
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to send the rich message with a table.", e);
        }

        // clientManager.sendMessage(botRole, SendMessage.builder().chatId(botRole.getUser().getId()).text("This is a test button.").build());
    }
}

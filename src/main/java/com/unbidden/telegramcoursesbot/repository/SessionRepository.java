package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.session.Session;
import java.util.List;
import org.springframework.lang.NonNull;

public interface SessionRepository extends CustomGeneralRepository<Integer, Session> {
    void removeForUser(@NonNull Long userId);

    void removeContentSessionsForUser(@NonNull Long userId);

    void removeUserOrChatRequestSessionsForUser(@NonNull Long userId);
    
    @NonNull
    List<Session> findForUser(@NonNull Long userId);
}

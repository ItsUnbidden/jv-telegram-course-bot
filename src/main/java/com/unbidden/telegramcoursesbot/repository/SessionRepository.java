package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.service.session.Session;
import java.util.List;
import org.springframework.lang.NonNull;

public interface SessionRepository extends CustomGeneralRepository<Integer, Session> {
    void removeForUserInBot(@NonNull Long userId, @NonNull Bot bot);

    void removeContentSessionsForUserInBot(@NonNull Long userId, @NonNull Bot bot);

    void removeSessionsWithoutConfirmationForUserInBot(@NonNull Long userId, @NonNull Bot bot);

    void removeUserOrChatRequestSessionsForUserInBot(@NonNull Long userId, @NonNull Bot bot);
    
    @NonNull
    List<Session> findForUserInBot(@NonNull Long userId, @NonNull Bot bot);
}

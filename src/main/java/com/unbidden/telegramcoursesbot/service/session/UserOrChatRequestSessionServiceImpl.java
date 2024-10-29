package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@RequiredArgsConstructor
public class UserOrChatRequestSessionServiceImpl implements UserOrChatRequestSessionService {
    private static final Logger LOGGER = LogManager
            .getLogger(UserOrChatRequestSessionServiceImpl.class);

    private final SessionRepository sessionRepository;

    @Override
    @NonNull
    public Integer createSession(@NonNull UserEntity user,
            @NonNull Consumer<List<Message>> function) {
        sessionRepository.removeContentSessionsForUser(user.getId());

        LOGGER.debug("Creating new user or chat request session for user "
                + user.getId() + "...");
        final UserOrChatRequestSession session = new UserOrChatRequestSession();
        session.setId(ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        session.setUser(user);
        session.setTimestamp(LocalDateTime.now());
        session.setFunction(function);
        sessionRepository.save(session);
        LOGGER.debug("Session saved.");
        return session.getId();
    }

    @Override
    public void removeSessionsForUser(@NonNull UserEntity user) {
        sessionRepository.removeForUser(user.getId());
    }

    @Override
    public void processResponse(@NonNull Session session, @NonNull Message message) {
        session.getFunction().accept(List.of(message));
    }
}

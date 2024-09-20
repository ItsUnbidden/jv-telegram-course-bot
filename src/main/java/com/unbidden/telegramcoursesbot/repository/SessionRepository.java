package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.session.Session;

public interface SessionRepository extends CustomGeneralRepository<Session> {
    void remove(String userId);
}

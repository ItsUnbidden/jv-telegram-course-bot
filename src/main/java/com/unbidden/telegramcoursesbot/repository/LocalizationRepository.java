package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public interface LocalizationRepository extends CustomGeneralRepository<String, Localization> {
    void clear();
}

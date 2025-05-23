package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.localization.Localization;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLocalizationRepository implements LocalizationRepository {
    private static final ConcurrentMap<String, Localization> localizations =
            new ConcurrentHashMap<>();

    @Override
    @NonNull
    public Localization save(@NonNull Localization localization) {
        localizations.put(localization.getName(), localization);
        return localization;
    }

    @Override
    @NonNull
    public Optional<Localization> find(@NonNull String id) {
        return Optional.ofNullable(localizations.get(id));
    }

    @Override
    public void clear() {
        localizations.clear();
    }
}

package com.unbidden.telegramcoursesbot.repository.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.unbidden.telegramcoursesbot.config.properties.SchedulingProperties;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MultipageListMeta;
import com.unbidden.telegramcoursesbot.repository.AutoClearable;
import com.unbidden.telegramcoursesbot.repository.MultipageListMetaRepository;

@Repository
public class InMemoryMultipageListMetaRepository implements MultipageListMetaRepository,
        AutoClearable {
    private static final Logger LOGGER = LogManager.getLogger(
            InMemoryMultipageListMetaRepository.class);

    private static final ConcurrentMap<UUID, MultipageListMeta> metas = new ConcurrentHashMap<>();

    private final MenuOrchestrationService menuService;

    private final SchedulingProperties schedulingProperties;

    public InMemoryMultipageListMetaRepository(@Lazy MenuOrchestrationService menuService,
            SchedulingProperties schedulingProperties) {
        this.menuService = menuService;
        this.schedulingProperties = schedulingProperties;
    }

    @Override
    @NonNull
    public MultipageListMeta save(@NonNull MultipageListMeta type) {
        metas.put(type.getId(), type);
        return type;
    }

    @Override
    @NonNull
    public Optional<MultipageListMeta> find(@NonNull UUID id) {
        return Optional.ofNullable(metas.get(id));
    }

    @Override
    public void removeExpired() {
        LOGGER.trace("Checking for expired multipage list metas...");
        final List<UUID> keysToRemove = new ArrayList<>();

        for (Entry<UUID, MultipageListMeta> entry : metas.entrySet()) {
            if (LocalDateTime.now().isAfter(entry.getValue()
                    .getCreatedAt().plusSeconds(schedulingProperties.multipageMeta().expiration()))) {
                LOGGER.trace("Terminating multipage list control menu for meta "
                        + entry.getKey() + "...");
                menuService.terminateMenu(entry.getValue().getUser().getId(),
                        entry.getValue().getMessageId(), entry.getValue().getBot());
                LOGGER.trace("Done. Adding key...");
                keysToRemove.add(entry.getKey());
            }
        }

        if (keysToRemove.isEmpty()) {
            LOGGER.trace("All multipage list metas are valid.");
            return;
        }
        LOGGER.trace("Some expired multipage list metas have been found.");
        for (UUID key : keysToRemove) {
            metas.remove(key);
        }
    }
}

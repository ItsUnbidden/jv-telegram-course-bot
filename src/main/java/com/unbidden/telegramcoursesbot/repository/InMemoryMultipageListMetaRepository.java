package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.MultipageListMeta;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMultipageListMetaRepository implements MultipageListMetaRepository,
        AutoClearable {
    private static final Logger LOGGER = LogManager.getLogger(
            InMemoryMultipageListMetaRepository.class);

    private static final ConcurrentMap<Integer, MultipageListMeta> metas =
            new ConcurrentHashMap<>();

    private static final int INITIAL_EXPIRY_CHECK_DELAY = 10000;

    @Autowired
    @Lazy
    private MenuService menuService;

    @Value("${telegram.bot.message.multipage.meta.expiration}")
    private Integer expiration;

    @Override
    @NonNull
    public MultipageListMeta save(@NonNull MultipageListMeta type) {
        metas.put(type.getId(), type);
        return type;
    }

    @Override
    @NonNull
    public Optional<MultipageListMeta> find(@NonNull Integer id) {
        return Optional.ofNullable(metas.get(id));
    }

    @Override
    @Scheduled(initialDelay = INITIAL_EXPIRY_CHECK_DELAY,
            fixedDelayString = "${telegram.bot.message.multipage.meta.schedule.delay}")
    public void removeExpired() {
        LOGGER.trace("Checking for expired sessions...");
        final List<Integer> keysToRemove = new ArrayList<>();

        for (Entry<Integer, MultipageListMeta> entry : metas.entrySet()) {
            if (LocalDateTime.now().isAfter(entry.getValue()
                    .getCreatedAt().plusSeconds(expiration))) {
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
        for (Integer key : keysToRemove) {
            metas.remove(key);
        }
    }
}

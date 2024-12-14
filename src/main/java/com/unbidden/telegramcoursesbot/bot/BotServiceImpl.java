package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.repository.BotRepository;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {
    private static final Logger LOGGER = LogManager.getLogger(BotServiceImpl.class);
    
    private static final String ERROR_CANNOT_CREATE_BOTFATHER = "error_cannot_create_botfather";
    private static final String ERROR_UNAVAILABLE_IN_REGULAR_BOT =
            "error_unavailable_in_regular_bot";

    private static final String BOT_FATHER_NAME = "botfather";

    private final BotRepository botRepository;

    private final BotRoleRepository botRoleRepository;

    private final LocalizationLoader localizationLoader;

    @Autowired
    @Lazy
    private ClientManager clientManager;

    @Autowired
    @Lazy
    private UserService userService;

    @Value("${telegram.bot.authorization.start_bot.token}")
    private String initialBotToken;

    @Value("${telegram.bot.authorization.start_bot.name}")
    private String initialBotName;

    @Value("${telegram.bot.authorization.bot_father.token}")
    private String botfatherToken;

    @Override
    @NonNull
    public Bot createBot(@NonNull UserEntity creator, @NonNull String name, @NonNull String token) {
        if (name.equals(BOT_FATHER_NAME)) {
            throw new ForbiddenOperationException("Botfather is absolute", localizationLoader
                    .getLocalizationForUser(ERROR_CANNOT_CREATE_BOTFATHER, creator));
        }
        LOGGER.info("Creating new bot " + name + "...");
        final Bot bot = new Bot();
        bot.setName(name);
        bot.setToken(token);

        final List<BotRole> botRoles = new ArrayList<>();
        final UserEntity director = userService.getDiretor();
        if (director.getId().equals(creator.getId())) {
            LOGGER.warn("Bot Creator is Director. No Creator role will be added.");
            botRoles.add(new BotRole(bot, director, userService.getRole(RoleType.DIRECTOR),
                    true));
        } else {
            botRoles.add(new BotRole(bot, creator, userService.getRole(RoleType.CREATOR), true));
            botRoles.add(new BotRole(bot, director,userService
                    .getRole(RoleType.DIRECTOR), false));
        }
        
        botRepository.save(bot);
        botRoleRepository.saveAll(botRoles);
        LOGGER.info("New bot " + name + " has been created. Initializing...");
        clientManager.addClient(bot);
        LOGGER.info("Client initialized for the new bot " + bot.getName() + ".");
        return bot;
    }

    @Override
    @NonNull
    public Bot createInitialBot(@NonNull UserEntity director) {
        try {
            return getBot(initialBotName);
        } catch (EntityNotFoundException e) {
            LOGGER.info("Initial bot does not exist. Creating...");
            return createBot(director, initialBotName, initialBotToken);
        }
    }

    @Override
    @NonNull
    public Bot createBotFather(@NonNull UserEntity director) {
        try {
            return getBotFather();
        } catch (EntityNotFoundException e) {
            LOGGER.info("Botfather does not exist. Creating...");
            final Bot botFather = new Bot();
            botFather.setName(BOT_FATHER_NAME);
            botFather.setToken(botfatherToken);
            botRepository.save(botFather);
            botRoleRepository.save(new BotRole(botFather, director, userService
                    .getRole(RoleType.DIRECTOR), false));
            LOGGER.info("Botfather has been created with id " + botFather.getId() + ".");
            return botFather;
        }
    }

    @Override
    @NonNull
    public List<Bot> initializeBots() {
        final List<Bot> bots = botRepository.findAll().stream()
                .filter(b -> !b.getName().equals(BOT_FATHER_NAME)).toList();
        bots.forEach(b -> clientManager.addClient(b));
        return bots;
    }

    @Override
    public void removeBot(@NonNull Bot bot) {
        botRepository.delete(bot);
    }

    @Override
    @NonNull
    public Bot getBot(@NonNull String name) {
        if (name.equals(BOT_FATHER_NAME)) {
            throw new EntityNotFoundException("Bot father cannot be fetched here", null);
        }
        return botRepository.findByName(name).orElseThrow(() ->
                new EntityNotFoundException("There is no bot with name " + name, null));
    }

    @Override
    @NonNull
    public Bot getBotFather() {
        return botRepository.findByName(BOT_FATHER_NAME).orElseThrow(() ->
                new EntityNotFoundException("Botfather has not been created yet", null));
    }

    @Override
    @NonNull
    public Bot getInitialBot() {
        return getBot(initialBotName);
    }

    @Override
    @NonNull
    public Bot initializeBotFather(@NonNull Bot bot) {
        clientManager.addBotFatherClient(bot);
        return bot;
    }

    @Override
    public void checkBotFather(@NonNull Bot bot, @NonNull UserEntity user) {
        if (!bot.getId().equals(getBotFather().getId())) {
            throw new AccessDeniedException("This action is available only from the "
                    + "botfather", localizationLoader.getLocalizationForUser(
                    ERROR_UNAVAILABLE_IN_REGULAR_BOT, user));
        }
    }

    @Override
    @NonNull
    public List<Bot> getAllBots() {
        return botRepository.findAll().stream()
                .filter(b -> !b.getName().equals(BOT_FATHER_NAME)).toList();
    }
}

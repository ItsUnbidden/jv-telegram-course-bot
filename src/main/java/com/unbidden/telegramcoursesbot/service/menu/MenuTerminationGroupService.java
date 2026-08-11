package com.unbidden.telegramcoursesbot.service.menu;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.unbidden.telegramcoursesbot.localization.Localizations.LocalizationKey;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.MenuTerminationGroup;
import com.unbidden.telegramcoursesbot.model.MessageEntity;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.MenuTerminationGroupRepository;
import com.unbidden.telegramcoursesbot.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuTerminationGroupService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(MenuTerminationGroupService.class);

    private final MenuTerminationGroupRepository menuTerminationGroupRepository;

    private final MessageRepository messageRepository;


    @Transactional
    public MenuTerminationGroup addToMenuTerminationGroup(UserEntity user,
            UserEntity messagedUser, Bot bot, Integer messageId,
            MenuTerminationGroupKey key, @Nullable LocalizationKey terminalLocalizationKey,
            Object[] args) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(messagedUser, "messagedUser cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messageId, "messageId cannot be null");
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(args, "args cannot be null");

        final Optional<MenuTerminationGroup> groupOpt = menuTerminationGroupRepository
                .findByUserIdAndName(user.getId(), key.getName().formatted(args));

        final MenuTerminationGroup group;
        if (groupOpt.isPresent()) {
            LOGGER.debug("MTG for user " + user.getId() + " and key " + key + " already exists.");
            group = groupOpt.get();
            
            group.getMessages().add(messageRepository.save(new MessageEntity(messagedUser,
                    messageId)));
        } else {
            LOGGER.debug("MTG for user " + user.getId() + " and key " + key + " does not exist yet.");
            group = new MenuTerminationGroup();
            
            group.setName(key.getName().formatted(args));
            group.setMessages(List.of(messageRepository.save(new MessageEntity(messagedUser, messageId))));
            group.setTerminalLocalizationName(terminalLocalizationKey != null ? terminalLocalizationKey.getLocName() : null);
            group.setUser(user);
            menuTerminationGroupRepository.save(group);
            LOGGER.debug("A new MTG " + group.getId() + " has been created.");
        }
        return group;
    }

    @Transactional
    public Optional<MenuTerminationGroup> terminateMenuGroup(UserEntity user, MenuTerminationGroupKey key, Object[] args) {
        final Optional<MenuTerminationGroup> groupOpt = menuTerminationGroupRepository.findByUserIdAndName(user.getId(), key.getName());
        final MenuTerminationGroup group;

        if (groupOpt.isPresent()) {
            group = groupOpt.get();
        } else {
            return groupOpt;
        }

        menuTerminationGroupRepository.delete(group);
        
        return groupOpt;
    }
}

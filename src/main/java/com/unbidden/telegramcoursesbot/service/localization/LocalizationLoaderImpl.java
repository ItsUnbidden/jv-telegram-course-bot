package com.unbidden.telegramcoursesbot.service.localization;

import com.unbidden.telegramcoursesbot.dao.LocalizationDao;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class LocalizationLoaderImpl implements LocalizationLoader {
    private static final Logger LOGGER = LogManager.getLogger(LocalizationLoaderImpl.class);

    @Value("${telegram.bot.message.text.format}")
    private String fileFormat;

    @Value("${telegram.bot.message.language.default}")
    private String defaultLanguageCode;

    @Value("${telegram.bot.message.text.path}")
    private String pathStr;

    private Path localizationFolderPath;

    @Autowired
    private TextUtil textUtil;

    @Autowired
    private LocalizationDao dao;

    @PostConstruct
    private void init() {
        localizationFolderPath = Path.of(System.getProperty("user.dir")).resolve(pathStr);
        LOGGER.info("Initialized localization files directory in "
                + localizationFolderPath + ".");
    }

    @Override
    @NonNull
    public String getLocTextForUser(@NonNull String name, @NonNull User user) {
        final Path pathToFileByDefaultLanguage = localizationFolderPath.resolve(name + "_"
                + defaultLanguageCode + fileFormat);
        final Path pathToFileByLanguage = localizationFolderPath.resolve(name + "_"
                + user.getLanguageCode() + fileFormat);

        LOGGER.info("File " + name + " is required for user " + user.getId()
                + ". Prefered language code is " + user.getLanguageCode() + ".");
        
        if (dao.exists(pathToFileByLanguage)) {
            LOGGER.info("User prefered language file is present. Loading file "
                    + pathToFileByLanguage + "...");
            return textUtil.injectUserData(dao.getText(
                    pathToFileByLanguage), user);
        }
        if (dao.exists(pathToFileByDefaultLanguage)) {
            LOGGER.info("User prefered language file is not present. Loading default file "
                    + pathToFileByDefaultLanguage + "...");
            return textUtil.injectUserData(dao.getText(
                    pathToFileByDefaultLanguage), user);
        }
        LOGGER.warn("Unknown localization file was requested. Default error message sent.");
        return name;
    }

    @Override
    @NonNull
    public String getLocTextForUser(@NonNull String name, @NonNull User user,
            Map<String, Object> parameterMap) {
        final String locTextForUser = getLocTextForUser(name, user);
        LOGGER.info("Injecting custom parameters...");
        return textUtil.injectParams(locTextForUser, parameterMap);
    }

    @Override
    @NonNull
    public SendMessage getSendMessage(@NonNull String name, @NonNull User user) {
        final String text = getLocTextForUser(name, user);
        return getSendMessage0(text, user);
    }

    @Override
    @NonNull
    public SendMessage getSendMessage(@NonNull String name, @NonNull User user,
            Map<String, Object> parameterMap) {
        final String text = getLocTextForUser(name, user, parameterMap);
        return getSendMessage0(text, user);
    }

    private SendMessage getSendMessage0(String text, User user) {
        final List<MessageEntity> entities = textUtil.getEntities(text);

        return SendMessage.builder()
                .chatId(user.getId())
                .text(textUtil.removeMarkers(text))
                .entities(entities)
                .build();
    }
}

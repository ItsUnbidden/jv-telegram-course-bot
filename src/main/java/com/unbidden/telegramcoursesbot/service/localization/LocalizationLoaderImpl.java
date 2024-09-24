package com.unbidden.telegramcoursesbot.service.localization;

import com.unbidden.telegramcoursesbot.dao.LocalizationDao;
import com.unbidden.telegramcoursesbot.exception.LocalizationLoadingException;
import com.unbidden.telegramcoursesbot.exception.TaggedStringInterpretationException;
import com.unbidden.telegramcoursesbot.repository.LocalizationRepository;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.FilenameUtils;
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

    @Autowired
    private LocalizationRepository localizationRepository;

    @PostConstruct
    private void init() {
        localizationFolderPath = Path.of(System.getProperty("user.dir")).resolve(pathStr);
        LOGGER.info("Initialized localization files directory in "
                + localizationFolderPath + ".");
        cacheLocalizationFiles();
    }

    @Override
    @NonNull
    public String getLocTextForUser(@NonNull String name, @NonNull User user) {
        
        return textUtil.injectUserData(loadLocalization(name, user.getLanguageCode())
                .getData(), user);
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

    @Override
    public void reloadResourses() {
        localizationRepository.clear();
        cacheLocalizationFiles();
    }

    private void cacheLocalizationFiles() {
        LOGGER.info("Localization files caching is commencing...");
        final List<Path> locDirs = dao.list(localizationFolderPath).stream()
                .filter(p -> p.toFile().isDirectory())
                .toList();

        LOGGER.info("Checking for default localization directory...");
        if (locDirs.stream()
                .filter(p -> p.getFileName().toString().equals(defaultLanguageCode))
                .toList()
                .isEmpty()) {
            throw new LocalizationLoadingException("No localization directory for "
                    + defaultLanguageCode + " was found by path " + localizationFolderPath);
        }
        LOGGER.info("There are " + locDirs.size() + " localization directories present: "
                + locDirs.stream().map(p -> p.getFileName().toString()).toList().toString()
                + " where default one is " + defaultLanguageCode);

        for (Path locDir : locDirs) {
            final List<Path> locFiles = dao.list(locDir).stream()
                    .filter(p -> p.toFile().isFile())
                    .toList();

            LOGGER.info("There are " + locFiles.size() + " localization files in "
                    + locDir.getFileName() + ": " + locFiles.stream()
                    .map(p -> p.getFileName().toString()).toList().toString() + ".");
            
            for (Path locFile : locFiles) {
                final String keyPattern = FilenameUtils.getBaseName(locFile.toString()) + "_%s_"
                        + locDir.getFileName();
                LOGGER.info("Working on file " + locFile.getFileName().toString()
                        + ". Key pattern is going to be: " + keyPattern.formatted("<tag>") + ".");

                try {
                    Map<String, String> tagedContent = textUtil.getMappedTagContent(
                            dao.getText(locFile));
                    LOGGER.info("Saving localization data...");
                    for (Entry<String, String> entry : tagedContent.entrySet()) {
                        localizationRepository.save(new Localization(keyPattern
                                .formatted(entry.getKey()), entry.getValue()));
                    }
                    LOGGER.info("Localization data from file " + locFile + " has been cached.");
                } catch (TaggedStringInterpretationException e) {
                    throw new LocalizationLoadingException("Unable to parse file " + locFile, e);
                }
            }
        }
        LOGGER.info("Localization files cached successfuly.");
    }

    private SendMessage getSendMessage0(String text, User user) {
        final List<MessageEntity> entities = textUtil.getEntities(text);

        return SendMessage.builder()
                .chatId(user.getId())
                .text(textUtil.removeMarkers(text))
                .entities(entities)
                .build();
    }

    @Override
    @NonNull
    public Localization loadLocalization(@NonNull String name, @NonNull String languageCode) {
        return localizationRepository.find(name + "_"
                + languageCode).orElse(localizationRepository.find(name + "_"
                + defaultLanguageCode).orElse(new Localization(name)));
    }
}

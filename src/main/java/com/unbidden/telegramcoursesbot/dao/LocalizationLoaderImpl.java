package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.UnableToReadTextFileException;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class LocalizationLoaderImpl implements LocalizationLoader {
    private static final Logger LOGGER = LogManager.getLogger(LocalizationLoaderImpl.class);

    private final TextUtil textUtil;

    @Value("${telegram.bot.message.text.format}")
    private String fileFormat;

    @Value("${telegram.bot.message.language.default}")
    private String defaultLanguageCode;

    @Value("${telegram.bot.message.text.path}")
    private String pathStr;

    private Path localizationFolderPath;

    @PostConstruct
    private void init() {
        localizationFolderPath = Path.of(System.getProperty("user.dir")).resolve(pathStr);
        LOGGER.info("Initialized localization files directory in "
                + localizationFolderPath + ".");
    }

    @Override
    @NonNull
    public String getTextByNameForUser(@NonNull String name, @NonNull User user) {
        Assert.hasText(name, "Name must not be blank");

        final Path pathToFileByDefaultLanguage = localizationFolderPath.resolve(name + "_"
                + defaultLanguageCode + fileFormat);
        final Path pathToFileByLanguage = localizationFolderPath.resolve(name + "_"
                + user.getLanguageCode() + fileFormat);

        LOGGER.info("File " + name + " is required for user " + user.getId()
                + ". Prefered language code is " + user.getLanguageCode() + ".");
        try {
            if (Files.exists(pathToFileByLanguage)) {
                LOGGER.info("User prefered language file is present. Loading file "
                        + pathToFileByLanguage + "...");
                return textUtil.injectUserData(new String(Files.readAllBytes(
                        pathToFileByLanguage), StandardCharsets.UTF_8), user);
            }
            if (Files.exists(pathToFileByDefaultLanguage)) {
                LOGGER.info("User prefered language file is not present. Loading default file "
                        + pathToFileByDefaultLanguage + "...");
                return textUtil.injectUserData(new String(Files.readAllBytes(
                        pathToFileByDefaultLanguage), StandardCharsets.UTF_8), user);
            }
            LOGGER.warn("Unknown localization file was requested. Default error message sent.");
            return name;
        } catch (IOException e) {
            throw new UnableToReadTextFileException("Unable to read file "
                    + name + fileFormat, e);
        }
    }
}

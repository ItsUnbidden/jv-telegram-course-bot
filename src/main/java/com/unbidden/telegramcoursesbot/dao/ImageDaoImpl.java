package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.FileDaoOperationException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ImageDaoImpl implements ImageDao {
    private static final String IMAGE_FORMAT = ".jpg";

    @Value("${telegram.bot.invoice.images.path}")
    private String imagePathStr;

    private Path imagePath;

    @PostConstruct
    public void init() {
        imagePath = Path.of(System.getProperty("user.dir")).resolve(imagePathStr);
    }

    @Override
    @NonNull
    public byte[] read(@NonNull String courseName) {
        final Path fileLocation = imagePath.resolve(courseName + IMAGE_FORMAT);

        try {
            return Files.readAllBytes(fileLocation);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to read the image by path "
                    + fileLocation, null, e);
        }
    }
}

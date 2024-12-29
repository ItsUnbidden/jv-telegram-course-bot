package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.FileDaoOperationException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.IOUtils;
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
        
        if (!exists(imagePath)) {
            createDir();
        }
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

    @Override
    @NonNull
    public Path addOrUpdateImage(@NonNull InputStream is, @NonNull String courseName) {
        final Path path = imagePath.resolve(courseName + IMAGE_FORMAT);

        OutputStream os = null;
        try {
            os = Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE);
            IOUtils.copy(is, os);
            os.flush();
            return path;
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to open an output stream for file "
                    + path.toString(), null);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    throw new FileDaoOperationException("Unable to close an output stream "
                            + "for file " + path.toString(), null);
                }
            }
            try {
                is.close();
            } catch (IOException e) {
                throw new FileDaoOperationException("Unable to close an input stream "
                        + "for file " + path.toString(), null);
            }
        }
    }

    @Override
    public boolean isPresent(@NonNull String courseName) {
        final Path fileLocation = imagePath.resolve(courseName + IMAGE_FORMAT);

        return exists(fileLocation);
    }

    @Override
    @NonNull
    public Path createDir() {
        try {
            return Files.createDirectories(imagePath);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to create default image "
                    + "directory on path " + imagePathStr, null, e);
        }
    }

    @Override
    public boolean exists(@NonNull Path path) {
        return Files.exists(path);
    }
}

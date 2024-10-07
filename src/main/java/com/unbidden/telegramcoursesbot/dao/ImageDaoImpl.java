package com.unbidden.telegramcoursesbot.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ImageDaoImpl implements ImageDao {
    @Override
    public byte[] read(@NonNull Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read the image by path " + path, e);
        }
    }
}

package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.FileDaoOperationException;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class LogDaoImpl implements LogDao {
    private static final String currentLogsFilePathStr = "logs/tcb.log";

    private Path currentLogsFilePath;

    @PostConstruct
    public void init() {
        currentLogsFilePath = Path.of(System.getProperty("user.dir"))
                .resolve(currentLogsFilePathStr);
    }

    @Override
    @NonNull
    public InputStream readCurrentLogFile() {
        try {
            if (Files.exists(currentLogsFilePath)) {
                return Files.newInputStream(currentLogsFilePath, StandardOpenOption.READ);
            }
            throw new FileDaoOperationException("Current log file does not exist", null);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to read log file", null, e);
        }
    }
}

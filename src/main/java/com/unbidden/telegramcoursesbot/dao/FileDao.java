package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.FileDaoOperationException;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.lang.NonNull;

public interface FileDao {
    default void closeStream(@NonNull InputStream is) {
        try {
            is.close();
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to close the stream.", null, e);
        }
    }
}

package com.unbidden.telegramcoursesbot.util;

import com.unbidden.telegramcoursesbot.exception.TaggedStringInterpretationException;
import jakarta.annotation.PostConstruct;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
public class TextUtil {
    private static final Logger LOGGER = LogManager.getLogger(TextUtil.class);

    private static final Map<String, String> MARKERS = new HashMap<>();
    private static final String FIRST_NAME_PATTERN = "${firstName}";
    private static final String LAST_NAME_PATTERN = "${lastName}";
    private static final String USERNAME_PATTERN = "${username}";
    private static final char TAG_OPEN = '<';
    private static final char TAG_CLOSE = '>';

    @PostConstruct
    public void init() {
        MARKERS.put("**", "bold");
        MARKERS.put("__", "italic");
        MARKERS.put("--", "underline");
        MARKERS.put("~~", "strikethrough");
        MARKERS.put("^^", "spoiler");
    }

    @NonNull
    public String injectUserData(@NonNull String text, @NonNull User user) {
        return text.replace(FIRST_NAME_PATTERN, user.getFirstName())
                .replace(LAST_NAME_PATTERN, user.getLastName())
                .replace(USERNAME_PATTERN, user.getUserName());
    }

    @NonNull
    public String injectParams(@NonNull String text, @NonNull Map<String, Object> params) {
        for (Entry<String, Object> entry : params.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue().toString());
        }
        return text;
    }

    @NonNull
    public Photo parsePhoto(@NonNull String rawFileStr) {
        final String[] data = rawFileStr.split("\n");

        if (data.length == 4) {
            Photo photo = new Photo();
            photo.setUrl(data[0]);
            photo.setSize(Integer.parseInt(data[1]));
            photo.setWidth(Integer.parseInt(data[2]));
            photo.setHeight(Integer.parseInt(data[3]));
            return photo;
        }
        throw new InvalidParameterException("Unable to parse photo in file.");
    }

    @NonNull
    public List<MessageEntity> getEntities(@NonNull String text) {
        final List<MessageEntity> entities = new ArrayList<>();

        for (Entry<String, String> entry : MARKERS.entrySet()) {
            int fromIndex = 0;
            int firstIndex = -1;

            while (text.indexOf(entry.getKey(), fromIndex) > 0) {
                if (firstIndex == -1) {
                    firstIndex = text.indexOf(entry.getKey(), fromIndex);
                    fromIndex = firstIndex + 2;
                    continue;
                }
                int secondIndex = text.indexOf(entry.getKey(), fromIndex);
                entities.add(MessageEntity.builder()
                        .type(entry.getValue())
                        .offset(firstIndex)
                        .length(secondIndex - firstIndex - 2)
                        .build());
                firstIndex = -1;
                fromIndex = secondIndex + 2;
            }
            text = text.replace(entry.getKey(), "");
        }

        final List<Integer> offsets = new ArrayList<>();
        final Map<Integer, List<MessageEntity>> collisionsMap = new HashMap<>();
        for (MessageEntity entity : entities) {
            if (collisionsMap.containsKey(entity.getOffset())) {
                collisionsMap.get(entity.getOffset()).add(entity);
            }
            ArrayList<MessageEntity> internalList = new ArrayList<>();
            internalList.add(entity);
            collisionsMap.put(entity.getOffset(), internalList);
            offsets.add(entity.getOffset());
        }
        offsets.sort(Comparator.naturalOrder());

        for (int i = 0; i < offsets.size(); i++) {
            final int currentOffsetFactor = i;
            collisionsMap.get(offsets.get(i))
                    .forEach(e -> e.setOffset(e.getOffset() - 4 * currentOffsetFactor));
        }
        return entities;
    }

    @NonNull
    public Map<String, String> getMappedTagContent(@NonNull String data)
            throws TaggedStringInterpretationException {
        LOGGER.info("Parsing tagged string...");
        final int[] chars = data.chars().toArray();
        final Map<String, String> result = new HashMap<>();

        boolean isRecording = false;
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < chars.length; i++) {
            if (isRecording && chars[i] == TAG_CLOSE && (i == 0 || chars[i - 1] != '\\')) {
                LOGGER.info("Current char " + (char)chars[i] + " on  position " + i
                        + ". Stopping recording of new tag...");
                final String tag = builder.toString();
                final int indexOfEndTag = data.indexOf(TAG_OPEN + builder.toString()
                        + "/" + TAG_CLOSE);

                isRecording = false;
                if (indexOfEndTag == -1) {
                    throw new TaggedStringInterpretationException("Unable to parse string. Tag "
                            + tag + " does not have a closing tag.");
                }

                final String locData = data.substring(i + 1, indexOfEndTag).trim();
                LOGGER.info("Tag is " + tag + ". End tag begins on " + indexOfEndTag
                        + ". Adding " + locData.length() + " chars to the map.");

                result.put(tag, locData);
                i = indexOfEndTag + tag.length() + 3;
                builder.delete(0, builder.length());
                LOGGER.info("New tag recording might begin anywhere from index "
                        + i + ". Tag builder cleared.");
            }
            if (isRecording) {
                builder.append((char)chars[i]);
            }
            if (chars[i] == TAG_OPEN && (i == 0 || chars[i - 1] != '\\')) {
                LOGGER.info("Current char " + (char)chars[i] + " on  position " + i
                        + ". Activating recording of new tag...");
                isRecording = true;
            }
        }
        if (isRecording) {
            throw new TaggedStringInterpretationException("File reading has been completed, "
                    + "but tag recording is still on.");
        }
        return result;
    }

    @NonNull
    public String removeMarkers(@NonNull String text) {
        for (Entry<String, String> entry : MARKERS.entrySet()) {
            text = text.replace(entry.getKey(), "");
        }
        return text;
    }
}

package com.unbidden.telegramcoursesbot.util;

import com.unbidden.telegramcoursesbot.exception.TaggedStringInterpretationException;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import jakarta.annotation.PostConstruct;
import java.security.InvalidParameterException;
import java.util.ArrayList;
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
    private static final String TAG_PARAMS_DIVIDER = " ";
    private static final String END_LINE_OVERRIDE_MARKER = "\\\n";

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
        return injectUserData0(text, user.getFirstName(), user.getLastName(), user.getUserName());
    }

    @NonNull
    public String injectUserData(@NonNull String text, @NonNull UserEntity user) {
        return injectUserData0(text, user.getFirstName(), user.getLastName(), user.getUsername());
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
        
        int offsetFactor0 = 0;
        int fromIndex = 0;

        while (true) {
            final MarkerDataDto markerData = getMarkerData(text, fromIndex);

            if (markerData.isEmpty) {
                break;
            }
            
            List<MessageEntity> stackedEntities = new ArrayList<>();
            extractEntities(markerData, stackedEntities);

            final int basicOffsetValue = markerData.beginsAt;
            final int offsetFactor = offsetFactor0;

            stackedEntities.forEach(e -> e.setOffset(basicOffsetValue - 4 * offsetFactor));
            entities.addAll(stackedEntities);
            offsetFactor0 += stackedEntities.size();
            fromIndex = markerData.endsAt;
        }
        return entities;
    }

    @NonNull
    public Map<Tag, String> getMappedTagContent(@NonNull String data)
            throws TaggedStringInterpretationException {
        LOGGER.info("Parsing tagged string...");
        final int[] chars = data.chars().toArray();
        final Map<Tag, String> result = new HashMap<>();

        boolean isRecording = false;
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < chars.length; i++) {
            if (isRecording && chars[i] == TAG_CLOSE && (i == 0 || chars[i - 1] != '\\')) {
                LOGGER.info("Current char " + (char)chars[i] + " on  position " + i
                        + ". Stopping recording of new tag...");
                final String[] splitTag = builder.toString().split(TAG_PARAMS_DIVIDER);
                final Tag tag = new Tag(splitTag[0], (splitTag.length > 1)
                        ? Boolean.valueOf(splitTag[1]) : false);
                final int indexOfEndTag = data.indexOf(TAG_OPEN + tag.getName()
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
                i = indexOfEndTag + tag.getName().length() + 2;
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

    @NonNull
    public String removeEndLineOverrides(@NonNull String text) {
        return text.replace(END_LINE_OVERRIDE_MARKER, "");
    }

    private int extractEntities(MarkerDataDto markerData, List<MessageEntity> entities) {
        if (markerData.isEmpty) {
            return markerData.data.length();
        }
        final int length = extractEntities(getMarkerData(markerData.data
                .replace(markerData.type, ""), 0), entities);
        entities.add(new MessageEntity(MARKERS.get(markerData.type), 0, length));
        return length;
    }

    private MarkerDataDto getMarkerData(String text, int fromIndex) {
        String type = "";
        int beginsAt = Integer.MAX_VALUE;
        for (Entry<String, String> marker : MARKERS.entrySet()) {
            int currentIndex = text.indexOf(marker.getKey(), fromIndex);
            if (currentIndex != -1 && currentIndex <= beginsAt) {
                beginsAt = currentIndex;
                type = marker.getKey();
            }
        }

        fromIndex = beginsAt + 2;
        if (!type.isEmpty()) {
            final int endsAt = text.indexOf(type, fromIndex) + 2;
            return new MarkerDataDto(type, beginsAt,
                    endsAt, text.substring(beginsAt, endsAt));
        }
        
        return new MarkerDataDto(text);
    }

    private String injectUserData0(String text, String firstName, String lastName,
            String username) {
        return text.replace(FIRST_NAME_PATTERN, firstName)
                .replace(LAST_NAME_PATTERN, (lastName == null) ? ""
                    : lastName)
                .replace(USERNAME_PATTERN, (username == null) ? ""
                    : username);
    }

    private static class MarkerDataDto {
        private String type;

        private int beginsAt;

        private int endsAt;

        private String data;

        private boolean isEmpty;

        private MarkerDataDto(String data) {
            this.data = data;
            this.isEmpty = true;
        }

        private MarkerDataDto(String type, int beginsAt, int endsAt, String data) {
            this.type = type;
            this.beginsAt = beginsAt;
            this.endsAt = endsAt;
            this.data = data;
            this.isEmpty = false;
        }
    }
}

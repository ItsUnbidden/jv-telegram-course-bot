package com.unbidden.telegramcoursesbot.util;

import com.unbidden.telegramcoursesbot.exception.TaggedStringInterpretationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Service;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Review;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

@Component
public class TextUtil {
    private static final Logger LOGGER = LogManager.getLogger(TextUtil.class);
    
    private static final Map<String, String> MARKERS = new HashMap<>();

    private static final char TAG_OPEN = '<';
    private static final char TAG_CLOSE = '>';
    private static final String TAG_PARAMS_DIVIDER = " ";
    private static final String END_LINE_OVERRIDE_MARKER = "\\\n";
    private static final String LANGUAGE_PRIORITY_DIVIDER = ",";
    private static final String PARAM_NAME_REGEX = "\\$\\{[a-zA-Z0-9_]+\\}";

    private final String languagePriorityStr;
    private final Pattern paramNamePattern;

    public TextUtil(@Value("${telegram.bot.message.language.priority}") String languagePriorityStr) {
        MARKERS.put("**", "bold");
        MARKERS.put("__", "italic");
        MARKERS.put("--", "underline");
        MARKERS.put("~~", "strikethrough");
        MARKERS.put("^^", "spoiler");

        this.languagePriorityStr = languagePriorityStr;
        this.paramNamePattern = Pattern.compile(PARAM_NAME_REGEX);
    }

    public String injectParams(String text, Map<String, Object> params) {
        for (Entry<String, Object> entry : params.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue().toString());
        }
        return text;
    }

    public List<MessageEntity> getEntities(String text) {
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

    public Map<Tag, String> getMappedTagContent(String data)
            throws TaggedStringInterpretationException {
        LOGGER.trace("Parsing tagged string...");
        final int[] chars = data.chars().toArray();
        final Map<Tag, String> result = new HashMap<>();

        boolean isRecording = false;
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < chars.length; i++) {
            if (isRecording && chars[i] == TAG_CLOSE && (i == 0 || chars[i - 1] != '\\')) {
                LOGGER.trace("Current char " + (char)chars[i] + " on  position " + i
                        + ". Stopping recording of new tag...");
                final String[] splitTag = builder.toString().split(TAG_PARAMS_DIVIDER);
                final Tag tag = new Tag(splitTag[0], (splitTag.length > 1)
                        ? Boolean.valueOf(splitTag[1]) : false);
                if (result.containsKey(tag)) {
                    throw new TaggedStringInterpretationException("Tag with name " + tag.getName()
                            + " is already present");
                }
                final int indexOfEndTag = data.indexOf(TAG_OPEN + tag.getName()
                        + "/" + TAG_CLOSE);

                isRecording = false;
                if (indexOfEndTag == -1) {
                    throw new TaggedStringInterpretationException("Unable to parse string. Tag "
                            + tag + " does not have a closing tag.");
                }

                final String locData = data.substring(i + 1, indexOfEndTag).trim();
                LOGGER.trace("Tag is " + tag + ". End tag begins on " + indexOfEndTag
                        + ". Adding " + locData.length() + " chars to the map.");

                result.put(tag, locData);
                i = indexOfEndTag + tag.getName().length() + 2;
                builder.delete(0, builder.length());
                LOGGER.trace("New tag recording might begin anywhere from index "
                        + i + ". Tag builder cleared.");
            }
            if (isRecording) {
                builder.append((char)chars[i]);
            }
            if (chars[i] == TAG_OPEN && (i == 0 || chars[i - 1] != '\\')) {
                LOGGER.trace("Current char " + (char)chars[i] + " on  position " + i
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

    public String removeMarkers(String text) {
        for (Entry<String, String> entry : MARKERS.entrySet()) {
            text = text.replace(entry.getKey(), "");
        }
        return text;
    }

    public String removeEndLineOverrides(String text) {
        return text.replace(END_LINE_OVERRIDE_MARKER, "");
    }

    public Set<String> getParamNames(String text) {
        final Matcher matcher = paramNamePattern.matcher(text);

        return matcher.results().map(mr -> mr.group()).collect(Collectors.toSet());
    }

    public String getArchiveReviewInfo(Review review, String localizedCourseName, StringBuilder builder) {
        LOGGER.info("Compiling review info for archive review " + review.getId() + "...");
        builder.append("Id: ").append(review.getId()).append("\n")
                .append("User: ").append(review.getUser().getFullName()).append("\n")
                .append("Course: ").append(localizedCourseName).append("\n")
                .append("Course grade: ").append(review.getCourseGrade()).append("\n")
                .append("Original course grade: ").append(review.getOriginalCourseGrade()).append("\n")
                .append("Basic review submitted at: ").append(
                    review.getBasicSubmittedTimestamp()).append("\n")
                .append("Advanced review content id: ").append((review.getContent() != null)
                    ? review.getContent().getId() : "Not available.").append("\n")
                .append("Advanced review original content id: ").append(
                    (review.getOriginalContent() != null) ? review.getOriginalContent()
                    .getId() : "Not available.").append("\n")
                .append("Advanced review submitted at: ").append((review
                    .getAdvancedSubmittedTimestamp() != null)
                    ? review.getAdvancedSubmittedTimestamp() : "Not available.").append("\n")
                .append("Last updated at: ").append((review.getLastUpdateTimestamp() != null)
                    ? review.getLastUpdateTimestamp() : "Not available.").append("\n")
                .append("Comment content id: ").append((review.getCommentContent() != null)
                    ? review.getCommentContent().getId() : "Not available.").append("\n")
                .append("Commented by: ").append((review.getCommentedBy() != null)
                    ? review.getCommentedBy().getFullName() : "Not available.").append("\n")
                .append("Commented at: ").append((review.getCommentedAt() != null)
                    ? review.getCommentedAt() : "Not available.").append("\n")
                .append("Users, who already marked this review as read: ").append(review
                    .getUsersWhoReadAsString()).append("\n");
        if (review.getContent() != null) {
            LOGGER.info("Review " + review.getId() + " contains advanced user feedback.");
            builder.append("Advanced review text: ").append("\n")
                    .append(review.getContent().getData()).append("\n");
        }
        builder.append("--------------------------------------------------------------------")
                .append("\n");
        return builder.toString();
    }

    public List<String> getLanguagePriority() {
        return Arrays.stream(
                languagePriorityStr.split(LANGUAGE_PRIORITY_DIVIDER))
                .map(lc -> lc.trim()).toList();
    }

    public String formatTimeLeft(BotRole botRole, LocalizationLoader loader, int hours) {
        if (hours > 1) {
            return loader.localize(Service.HOURS, botRole, new Service.HoursParams(hours)).getData();
        }
        if (hours == 1) {
            return loader.localize(Service.AN_HOUR, botRole).getData();
        }
        if (hours <= 0) {
            return loader.localize(Service.LESS_THEN_AN_HOUR, botRole).getData();
        }
        return String.valueOf(hours);
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

package com.unbidden.telegramcoursesbot.util;

import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.TaggedStringInterpretationException;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public class TextUtil {
    private static final Logger LOGGER = LogManager.getLogger(TextUtil.class);
    
    private static final Map<String, String> MARKERS = new HashMap<>();
    
    private static final String PARAM_LAST_UPDATE_TIMESTAMP = "${lastUpdateTimestamp}";
    private static final String PARAM_USER_FULL_NAME = "${userFullName}";
    private static final String PARAM_ADVANCED_TIMESTAMP = "${advancedTimestamp}";
    private static final String PARAM_ORIGINAL_CONTENT_ID = "${originalContentId}";
    private static final String PARAM_CONTENT_ID = "${contentId}";
    private static final String PARAM_COMMENTED_AT = "${commentedAt}";
    private static final String PARAM_USER_WHO_COMMENTED = "${userWhoCommented}";
    private static final String PARAM_USERS_WHO_READ = "${usersWhoRead}";
    private static final String PARAM_ORIGINAL_PLATFORM_GRADE = "${originalPlatformGrade}";
    private static final String PARAM_ORIGINAL_COURSE_GRADE = "${originalCourseGrade}";
    private static final String PARAM_PLATFORM_GRADE = "${platformGrade}";
    private static final String PARAM_COURSE_GRADE = "${courseGrade}";
    private static final String PARAM_BASIC_TIMESTAMP = "${basicTimestamp}";
    private static final String PARAM_MESSAGE_INDEX = "${messageIndex}";
    private static final String PARAM_PROVIDED_MESSAGES_AMOUNT = "${providedMessagesNumber}";
    private static final String PARAM_EXPECTED_MESSAGES_AMOUNT = "${expectedMessagesAmount}";
    private static final String FIRST_NAME_PATTERN = "${firstName}";
    private static final String LAST_NAME_PATTERN = "${lastName}";
    private static final String USERNAME_PATTERN = "${username}";
    private static final char TAG_OPEN = '<';
    private static final char TAG_CLOSE = '>';
    private static final String TAG_PARAMS_DIVIDER = " ";
    private static final String END_LINE_OVERRIDE_MARKER = "\\\n";
    private static final String LANGUAGE_PRIORITY_DIVIDER = ",";

    private static final String SERVICE_LESS_THEN_AN_HOUR = "service_less_then_an_hour";

    private static final String ERROR_MESSAGE_TEXT_MISSING = "error_message_text_missing";
    private static final String ERROR_AMOUNT_OF_MESSAGES = "error_amount_of_messages";

    @Value("${telegram.bot.message.language.priority}")
    private String languagePriorityStr;

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

    @NonNull
    public String getArchiveReviewInfo(@NonNull Review review, @NonNull StringBuilder builder) {
        LOGGER.info("Compiling review info for archive review " + review.getId() + "...");
        builder.append("Id: ").append(review.getId()).append("\n")
                .append("User: ").append(review.getUser().getFullName()).append("\n")
                .append("Course: ").append(review.getCourse().getName()).append("\n")
                .append("Course grade: ").append(review.getCourseGrade()).append("\n")
                .append("Platform grade: ").append(review.getPlatformGrade()).append("\n")
                .append("Original course grade: ").append(review.getOriginalCourseGrade())
                .append("\n")
                .append("Original platform grade: ").append(review.getOriginalPlatformGrade())
                .append("\n")
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

    @NonNull
    public Map<String, Object> getParamsMapForNewReview(@NonNull Review review) {
        final Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(PARAM_USER_FULL_NAME, review.getUser().getFullName());
        parameterMap.put(PARAM_BASIC_TIMESTAMP, review.getBasicSubmittedTimestamp());
        parameterMap.put(PARAM_COURSE_GRADE, review.getCourseGrade());
        parameterMap.put(PARAM_PLATFORM_GRADE, review.getPlatformGrade());
        parameterMap.put(PARAM_ORIGINAL_COURSE_GRADE, review.getOriginalCourseGrade());
        parameterMap.put(PARAM_ORIGINAL_PLATFORM_GRADE, review.getOriginalPlatformGrade());
        parameterMap.put(PARAM_USERS_WHO_READ, review.getUsersWhoReadAsString());
        parameterMap.put(PARAM_LAST_UPDATE_TIMESTAMP, (review.getLastUpdateTimestamp() != null)
                ? review.getLastUpdateTimestamp() : "Not available");

        if (review.getCommentContent() != null) {
            parameterMap.put(PARAM_USER_WHO_COMMENTED, review.getCommentedBy()
                    .getFullName());
            parameterMap.put(PARAM_COMMENTED_AT, review.getCommentedAt());
        }

        if (review.getContent() != null) {
            parameterMap.put(PARAM_CONTENT_ID, review.getContent().getId());
            parameterMap.put(PARAM_ORIGINAL_CONTENT_ID, review.getOriginalContent().getId());
            parameterMap.put(PARAM_ADVANCED_TIMESTAMP, review.getAdvancedSubmittedTimestamp());
        }
        return parameterMap;
    }

    @NonNull
    public String[] getLanguagePriority() {
        return languagePriorityStr.split(LANGUAGE_PRIORITY_DIVIDER);
    }

    @NonNull
    public String formatTimeLeft(@NonNull UserEntity user, @NonNull LocalizationLoader loader,
            int hours) {
        if (hours <= 0) {
            return loader.getLocalizationForUser(SERVICE_LESS_THEN_AN_HOUR, user).getData();
        }
        return String.valueOf(hours);
    }

    public void checkExpectedMessages(int amount, @NonNull UserEntity user,
            @NonNull List<Message> messages, @NonNull LocalizationLoader loader) {
        if (messages.size() != amount) {
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_EXPECTED_MESSAGES_AMOUNT, amount);
            parameterMap.put(PARAM_PROVIDED_MESSAGES_AMOUNT, messages.size());

            throw new InvalidDataSentException("There are supposed to be "
                    + amount + " messages. User " + user.getId()
                    + " has sent " + messages.size() + " messages though.",
                    loader.getLocalizationForUser(
                    ERROR_AMOUNT_OF_MESSAGES, user, parameterMap));
        }
        for (int i = 0; i < messages.size(); i++) {
            if (!messages.get(i).hasText()) {
                throw new InvalidDataSentException("Message " + messages.get(i)
                        .getMessageId() + " sent by user " + user.getId()
                        + " does not have any text.",
                        loader.getLocalizationForUser(
                        ERROR_MESSAGE_TEXT_MISSING, user, PARAM_MESSAGE_INDEX, i));
            }
        }
        LOGGER.debug("Prelimenary checks have been completed. "
                + "Trying to set variables...");
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

package com.unbidden.telegramcoursesbot.util;

import com.unbidden.telegramcoursesbot.config.properties.LocalizationsProperties;
import com.unbidden.telegramcoursesbot.exception.LocalizationLoadingException;
import com.unbidden.telegramcoursesbot.exception.TaggedStringInterpretationException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Tag;
import com.unbidden.telegramcoursesbot.localization.Localizations.Service;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Review;

import lombok.EqualsAndHashCode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
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
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.richtext.RichText;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextBold;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextConcat;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextItalic;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextPlain;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextSpoiler;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextStrikethrough;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextUnderline;

@Component
public class TextUtil {
    private static final Logger LOGGER = LogManager.getFormatterLogger(TextUtil.class);
    
    private static final Map<String, String> MARKERS = new HashMap<>();

    private static final char TAG_OPEN = '<';
    private static final char TAG_CLOSE = '>';
    private static final String TAG_PARAMS_DIVIDER = " ";
    private static final String END_LINE_OVERRIDE_MARKER = "\\\n";
    private static final String LANGUAGE_PRIORITY_DIVIDER = ",";
    private static final String END_TAG_INDICATOR = "/";
    private static final String PARAM_NAME_REGEX = "\\$\\{[a-zA-Z0-9_]+\\}";

    private static final Comparator<MessageEntity> ME_COMPARATOR = (me1, me2) -> {
        if (me1.getOffset() > me2.getOffset()) return 1;
        if (me1.getOffset() < me2.getOffset()) return -1;

        if (me1.getLength() < me2.getLength()) return 1;
        if (me1.getLength() > me2.getLength()) return -1;

        return 0;
    };

    private final List<String> languagePriority;

    private final Pattern paramNamePattern;

    public TextUtil(LocalizationsProperties localizationsProperties) {
        MARKERS.put("**", "bold");
        MARKERS.put("__", "italic");
        MARKERS.put("--", "underline");
        MARKERS.put("~~", "strikethrough");
        MARKERS.put("^^", "spoiler");

        this.languagePriority = Arrays.stream(
                localizationsProperties.languagePriority().split(LANGUAGE_PRIORITY_DIVIDER))
                .map(lc -> lc.trim()).toList();
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
        final List<MarkerDataDto> markersData = new ArrayList<>();
        
        for (final String marker : MARKERS.keySet()) {
            for (int i = 0; i < text.length();) {
                final int occurence = text.indexOf(marker, i);

                if (occurence == -1) break;

                i = occurence + 2;
                markersData.add(new MarkerDataDto(occurence, marker));
            }
        }
        markersData.sort((m1, m2) -> {
            if (m1.offset > m2.offset) return 1;
            if (m1.offset < m2.offset) return -1;
            return 0;
        });
        
        for (int i = 0; i < markersData.size(); ++i) {
            markersData.get(i).offset -= 2 * i;
        }

        while (markersData.size() > 0) {
            final var targetMarker = markersData.removeFirst();
            final var endMarker = markersData.stream().filter(m -> m.type.equals(targetMarker.type)).findFirst().orElseThrow(
                    () -> new LocalizationLoadingException("There is no closing marker of type " + targetMarker.type
                    + ". Opening marker is located at offset " + targetMarker.offset + ".", null));

            markersData.remove(endMarker);

            entities.add(new MessageEntity(MARKERS.get(targetMarker.type), targetMarker.offset, endMarker.offset - targetMarker.offset));
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
                        + END_TAG_INDICATOR + TAG_CLOSE);

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

    public String generateLocalizationTemplate(Map<String, List<String>> info) {
        final StringBuilder builder = new StringBuilder();

        for (final Entry<String, List<String>> entry : info.entrySet()) {
            builder.append(TAG_OPEN).append(entry.getKey()).append(TAG_CLOSE).append('\n');

            for (final String paramName : entry.getValue()) {
                builder.append("${").append(paramName).append('}').append(' ');
            }
            builder.append('\n').append(TAG_OPEN).append(entry.getKey()).append(END_TAG_INDICATOR).append(TAG_CLOSE)
                    .append('\n').append('\n');
        }

        return builder.toString();
    }

    public List<String> getLanguagePriority() {
        return languagePriority;
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

    public RichTextConcat getRichTextFromLocalization(Localization loc) {
        final var builder = RichTextConcat.builder();
        final List<TextEntitiesPair> pairs = splitStringAtBreakpoint(loc.getData(), loc.getEntities());

        for (int i = 0; i < pairs.size(); ++i) {
            builder.text(parseEntitiesToRichText(pairs.get(i).text, nestEntities(pairs.get(i).entities), "plain", 0, pairs.get(i).text.length()));
        }

        return builder.build();
    }

    private List<TextEntitiesPair> splitStringAtBreakpoint(String text, List<MessageEntity> entities) {
        entities.sort(ME_COMPARATOR);

        int breakpoint = -1;
        for (final var entity : entities) {
            final int end = entity.getOffset() + entity.getLength();

            if (entities.stream().anyMatch(e -> end < e.getOffset() + e.getLength()
                    && entity.getOffset() < e.getOffset()
                    && end > e.getOffset())) {
                breakpoint = end;
                break;
            }
        }

        if (breakpoint == -1) return List.of(new TextEntitiesPair(text, entities));
        final List<MessageEntity> part1Entities = new ArrayList<>();
        final List<MessageEntity> part2Entities = new ArrayList<>();

        for (final var entity : entities) {
            final int end = entity.getOffset() + entity.getLength();

            if (entity.getOffset() < breakpoint) {
                part1Entities.add(new MessageEntity(entity.getType(), entity.getOffset(),
                        Math.clamp(end, 0, breakpoint) - entity.getOffset()));
            }
            if (end > breakpoint) {
                int newStart = 0;
                if (entity.getOffset() > breakpoint) {
                    newStart = entity.getOffset() - breakpoint;
                }
                part2Entities.add(new MessageEntity(entity.getType(), newStart, end - breakpoint - newStart));
            }
        }
        part2Entities.sort(ME_COMPARATOR);

        final List<TextEntitiesPair> pairs = new ArrayList<>();

        pairs.add(new TextEntitiesPair(text.substring(0, breakpoint), part1Entities));
        pairs.addAll(splitStringAtBreakpoint(text.substring(breakpoint), part2Entities));

        return pairs;
    }

    private List<NestedMessageEntity> nestEntities(List<MessageEntity> entities) {
        final List<NestedMessageEntity> result = new ArrayList<>();

        if (entities.isEmpty()) return result;

        final Deque<NestedMessageEntity> stack = new ArrayDeque<>();

        for (final MessageEntity entity : entities) {
            final var lastEntity = stack.peekFirst();
            final NestedMessageEntity newEntity = new NestedMessageEntity(entity.getType(), entity.getOffset(), entity.getOffset() + entity.getLength(), new ArrayList<>());

            if (lastEntity == null) {
                stack.addFirst(newEntity);
            } else if (entity.getOffset() >= lastEntity.end) {
                do {
                    final NestedMessageEntity popped = stack.removeFirst();

                    if (stack.isEmpty()) result.add(popped);
                } while (!stack.isEmpty() && entity.getOffset() >= stack.getFirst().end);

                if (!stack.isEmpty()) stack.getFirst().entities.add(newEntity);
                stack.addFirst(newEntity);
            } else {
                lastEntity.entities.add(newEntity);
                stack.addFirst(newEntity);
            }
        }
        result.add(stack.getLast());

        return result;
    }

    private RichText parseEntitiesToRichText(String text, List<NestedMessageEntity> entities, String type, int start, int end) {
        if (entities.isEmpty()) return getRichText(new RichTextPlain(text.substring(start, end)), type);

        final var concat = RichTextConcat.builder();

        for (final NestedMessageEntity entity : entities) {
            final String startSubstring = text.substring(start, entity.start);

            if (!startSubstring.isEmpty()) concat.text(new RichTextPlain(startSubstring));
            concat.text(parseEntitiesToRichText(text, entity.entities, entity.type, entity.start, entity.end));
            start = entity.end;
        }
        final String endSubstring = text.substring(start, end);

        if (!endSubstring.isEmpty()) concat.text(new RichTextPlain(endSubstring));

        return getRichText(concat.build(), type);
    }

    private RichText getRichText(RichText text, String type) {
        switch (type) {
            case "bold" -> {
                return RichTextBold.builder().text(text).build();
            }
            case "italic" -> {
                return RichTextItalic.builder().text(text).build();
            }
            case "underline" -> {
                return RichTextUnderline.builder().text(text).build();
            }
            case "strikethrough" -> {
                return RichTextStrikethrough.builder().text(text).build();
            }
            case "spoiler" -> {
                return RichTextSpoiler.builder().text(text).build();
            }
            default -> {
                return text;
            }
        }
    }

    private static record NestedMessageEntity(String type, int start, int end, List<NestedMessageEntity> entities) {}
    private static record TextEntitiesPair(String text, List<MessageEntity> entities) {}

    @EqualsAndHashCode
    private static class MarkerDataDto {
        int offset;
        
        final String type;

        public MarkerDataDto(int offset, String type) {
            this.offset = offset;
            this.type = type;
        }
    }
}

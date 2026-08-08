package com.unbidden.telegramcoursesbot.localization;

import java.time.LocalDateTime;
import java.util.List;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;

public class Localizations {
    public static LocalizationKey getKeyByName(String name) {
        try {
            return Menu.findKeyByName(name);
        } catch (RuntimeException e) {
        }
        try {
            return Button.findKeyByName(name);
        } catch (RuntimeException e) {
        }
        try {
            return Service.findKeyByName(name);
        } catch (RuntimeException e) {
        }
        try {
            return Error.findKeyByName(name);
        } catch (RuntimeException e) {
            throw new RuntimeException("Unable to find localization \"" + name + "\".");
        }
    }

    public static interface LocalizationKey {
        String getLocName();
    }

    public static enum Menu implements LocalizationKey {
        COMMIT_CONTENT_EXPIRED_TERMINAL_PAGE("menu_commit_content_expired_terminal_page");

        private String locName;

        Menu(String locName) {
            this.locName = locName;
        }

        @Override
        public String getLocName() {
            return locName;
        }

        public static Menu findKeyByName(String name) {
            if (name == null || name.isBlank()) throw new RuntimeException("Name must not be null or blank.");

            for (final Menu menu : values()) {
                if (menu.toString().equals(name)) return menu;
            }
            throw new RuntimeException("Unable to find menu localization \"" + name + "\".");
        }
    }

    public static enum Button implements LocalizationKey {
        _FDF("");

        private String locName;

        Button(String locName) {
            this.locName = locName;
        }

        public String getLocName() {
            return locName;
        }

        public static Button findKeyByName(String name) {
            if (name == null || name.isBlank()) throw new RuntimeException("Name must not be null or blank.");
            
            for (final Button button : values()) {
                if (button.toString().equals(name)) return button;
            }
            throw new RuntimeException("Unable to find button localization \"" + name + "\".");
        }
    }

    public static enum Service implements LocalizationKey {
        ROLE_DIRECTOR_TITLE("service_role_director_title"),
        ROLE_CREATOR_TITLE("service_role_creator_title"),
        ROLE_MENTOR_TITLE("service_role_mentor_title"),
        ROLE_SUPPORT_TITLE("service_role_support_title"),
        ROLE_USER_TITLE("service_role_user_title"),
        ROLE_BANNED_TITLE("service_role_banned_title"),
        NOT_AVAILABLE("service_not_available"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>currentPage</li>
         *  <li>numberOfPages</li>
         *  <li>numberOfElements</li>
         *  <li>data</li>
         *  <li>courseName</li>
         * </ls>
         */
        COURSE_COMPLETED_USERS("service_course_completed_users"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>whoBanned</li>
         * </ls>
         */
        BANNED("service_banned"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>hoursUntilLift</li>
         *  <li>whoBanned</li>
         * </ls>
         */
        TEMPORARY_BANNED("service_temporary_banned"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>newRoleType</li>
         *  <li>whoChanged</li>
         * </ls>
         */
        ROLE_CHANGED("service_role_changed"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>whoLifted</li>
         * </ls>
         */
        BAN_LIFTED("service_ban_lifted"),
        BAN_LIFTED_AUTO("service_ban_lifted_auto"),
        GENERAL_BAN_LIFTED_AUTO("service_general_ban_lifted_auto"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>whoLifted</li>
         * </ls>
         */
        GENERAL_BAN_LIFTED("service_general_ban_lifted"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>hoursUntilLift</li>
         *  <li>whoBanned</li>
         * </ls>
         */
        TEMPORARY_GENERAL_BAN("service_temporary_general_ban"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>whoBanned</li>
         * </ls>
         */
        GENERAL_BAN("service_general_ban"),
        LESS_THEN_AN_HOUR("service_less_then_an_hour"),
        AN_HOUR("service_an_hour"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>hours</li>
         * </ls>
         */
        HOURS("service_hours"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>basicTimestamp</li>
         *  <li>lastUpdateTimestamp</li>
         *  <li>courseName</li>
         *  <li>courseGrade</li>
         *  <li>platformGrade</li>
         *  <li>usersWhoRead</li>
         *  <li>userWhoCommented</li>
         *  <li>commentedAt</li>
         *  <li>contentId</li>
         *  <li>advancedTimestamp</li>
         * </ls>
         */
        REVIEW_INFO_CONTENT_COMMENT("service_review_info_content_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>basicTimestamp</li>
         *  <li>lastUpdateTimestamp</li>
         *  <li>courseName</li>
         *  <li>courseGrade</li>
         *  <li>platformGrade</li>
         *  <li>usersWhoRead</li>
         *  <li>userWhoCommented</li>
         *  <li>commentedAt</li>
         * </ls>
         */
        REVIEW_INFO_CONTENT("service_review_info_content_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>basicTimestamp</li>
         *  <li>lastUpdateTimestamp</li>
         *  <li>courseName</li>
         *  <li>courseGrade</li>
         *  <li>platformGrade</li>
         *  <li>usersWhoRead</li>
         *  <li>contentId</li>
         *  <li>advancedTimestamp</li>
         * </ls>
         */
        REVIEW_INFO_COMMENT("service_review_info_content_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>basicTimestamp</li>
         *  <li>lastUpdateTimestamp</li>
         *  <li>courseName</li>
         *  <li>courseGrade</li>
         *  <li>platformGrade</li>
         *  <li>usersWhoRead</li>
         * </ls>
         */
        REVIEW_INFO("service_review_info_content_comment"),
        NO_NEW_REVIEWS_FOR_USER("service_no_new_reviews_for_user"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REVIEW_CONTENT_UPDATED("service_review_content_updated"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REVIEW_PLATFORM_GRADE_UPDATED("service_review_platform_grade_updated"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REVIEW_COURSE_GRADE_UPDATED("service_review_course_grade_updated"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>commenterFullName</li>
         *  <li>title</li>
         * </ls>
         */
        COMMENT_SUBMITTED_NOTIFICATION("service_comment_submitted_notification"),
        COMMENT_SUBMITTED("service_comment_submitted"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        ADVANCED_REVIEW_SUBMITTED("service_advanced_review_submitted"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        BASIC_REVIEW_SUBMITTED("service_basic_review_submitted"),
        ADVANCED_REVIEW_TERMINAL("service_advanced_review_terminal"),
        BASIC_REVIEW_TERMINAL("service_basic_review_terminal"),
        REVIEW_MEDIA_GROUP_BYPASS("service_review_media_group_bypass"),
        SEND_HOMEWORK_MEDIA_GROUP_BYPASS("service_send_homework_media_group_bypass"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>targetId</li>
         *  <li>targetFullName</li>
         *  <li>targetLanguageCode</li>
         *  <li>courseName</li>
         *  <li>lessonIndex</li>
         * </ls>
         */
        HOMEWORK_FEEDBACK_REQUEST_NOTIFICATION("service_homework_feedback_request_notification"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>lessonIndex</li>
         *  <li>whoDeclined</li>
         *  <li>title</li>
         * </ls>
         */
        HOMEWORK_DECLINED_NOTIFICATION_PLUS_COMMENT("service_homework_declined_notification_plus_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>lessonIndex</li>
         *  <li>whoApproved</li>
         *  <li>title</li>
         * </ls>
         */
        HOMEWORK_APPROVED_NOTIFICATION("service_homework_approved_notification"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>lessonIndex</li>
         *  <li>whoApproved</li>
         *  <li>title</li>
         * </ls>
         */
        HOMEWORK_APPROVED_NOTIFICATION_PLUS_COMMENT("service_homework_approved_notification_plus_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>targetId</li>
         *  <li>targetFullName</li>
         *  <li>targetLanguageCode</li>
         * </ls>
         */
        HOMEWORK_SUBMITTED_NOTIFICATION("service_homework_submitted_notification"),
        HOMEWORK_ACCEPTED_AUTO("service_homework_accepted_auto"),
        FEEDBACK_FOR_HOMEWORK_WAITING("service_feedback_for_homework_waiting"),
        FEEDBACK_MEDIA_GROUP_BYPASS("service_feedback_media_group_bypass"),
        SUPPORT_REQUEST_MEDIA_GROUP_BYPASS("service_support_request_media_group_bypass"),
        SUPPORT_REPLY_MEDIA_GROUP_BYPASS("service_support_reply_media_group_bypass"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>title</li>
         * </ls>
         */
        SUPPORT_REPLY_INFO("service_support_reply_info"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>timestamp</li>
         *  <li>tag</li>
         * </ls>
         */
        SUPPORT_INFO("service_support_info"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>title</li>
         * </ls>
         */
        SUPPORT_REQUEST_RESOLVED("service_support_request_resolved"),
        COURSE_NEXT_STAGE_MEDIA_GROUP_BYPASS("service_course_next_stage_media_group_bypass");

        private String locName;

        Service(String locName) {
            this.locName = locName;
        }

        @Override
        public String getLocName() {
            return locName;
        }

        public static Service findKeyByName(String name) {
            if (name == null || name.isBlank()) throw new RuntimeException("Name must not be null or blank.");    

            for (final Service service : values()) {
                if (service.toString().equals(name)) return service;
            }
            throw new RuntimeException("Unable to find service localization \"" + name + "\".");
        }

        public static record CourseCompletedUsersParams(int currentPage, int numberOfPages, long numberOfElements, String data, String courseName) {}
        public static record BannedParams(String whoBanned, String title) {}
        public static record TemporaryBannedParams(String whoBanned, int hoursUntilLift, String title) {}
        public static record RoleChangedParams(String whoChanged, RoleType newRoleType, String title) {}
        public static record BanLiftedParams(String whoLifted, String title) {}
        public static record GeneralBanLiftedParams(String whoLifted, String title) {}
        public static record TemporaryGeneralBanParams(String whoBanned, int hoursUntilLift, String title) {}
        public static record GeneralBanParams(String whoBanned, String title) {}
        public static record HoursParams(int hours) {}
        public static record ReviewInfoContentCommentParams(String userFullName, LocalDateTime basicTimestamp,
                LocalDateTime lastUpdateTimestamp, String courseName, int courseGrade, int platformGrade, String usersWhoRead,
                String userWhoCommented, LocalDateTime commentedAt, long contentId, LocalDateTime advancedTimestamp) {}
        public static record ReviewInfoContentParams(String userFullName, LocalDateTime basicTimestamp,
                LocalDateTime lastUpdateTimestamp, String courseName, int courseGrade, int platformGrade,
                String usersWhoRead, long contentId, LocalDateTime advancedTimestamp) {}
        public static record ReviewInfoCommentParams(String userFullName, LocalDateTime basicTimestamp,
                LocalDateTime lastUpdateTimestamp, String courseName, int courseGrade, int platformGrade,
                String usersWhoRead, String userWhoCommented, LocalDateTime commentedAt) {}
        public static record ReviewInfoParams(String userFullName, LocalDateTime basicTimestamp, LocalDateTime lastUpdateTimestamp,
                String courseName, int courseGrade, int platformGrade, String usersWhoRead) {}
        public static record ReviewContentUpdatedParams(String courseName) {}
        public static record ReviewPlatformGradeUpdatedParams(String courseName) {}
        public static record ReviewCourseGradeUpdatedParams(String courseName) {}
        public static record CommentSubmittedNotificationParams(String courseName, String commenterFullName, String title) {}
        public static record BasicReviewSubmittedParams(String courseName) {}
        public static record AdvancedReviewSubmittedParams(String courseName) {}
        public static record HomeworkFeedbackRequestNotificationParams(Long targetId, String targetFullName, String targetLanguageCode, String courseName, int lessonIndex) {}
        public static record HomeworkDeclinedNotificationPlusCommentParams(String courseName, int lessonIndex, String whoApproved, String title) {}
        public static record HomeworkApprovedNotificationParams(String courseName, int lessonIndex, String whoApproved, String title) {}
        public static record HomeworkApprovedNotificationPlusCommentParams(String courseName, int lessonIndex, String whoApproved, String title) {}
        public static record HomeworkSubmittedNotificationParams(Long targetId, String targetFullName, String targetLanguageCode) {}
        public static record SupportReplyInfoParams(String userFullName, String title) {}
        public static record SupportInfoParams(String userFullName, LocalDateTime timestamp, String tag) {}
        public static record SupportRequestResolvedParams(String userFullName, String title) {}
    }

    public static enum Error implements LocalizationKey {
        /**
         * Possible parameters:
         * <ls>
         *  <li>messageIndex</li>
         * </ls>
         */
        MESSAGE_TEXT_MISSING("error_message_text_missing"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>messageIndex</li>
         * </ls>
         */
        NUMBER_OF_MESSAGES("error_number_of_messages"),
        USER_IS_BANNED_IN_BOT("error_user_is_banned_in_bot"),
        USER_NOT_REGISTRED("error_user_not_registred"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>excMessage</li>
         *  <li>excClassName</li>
         * </ls>
         */
        NO_EXCEPTION_LOCALIZATION_AVAILABLE("error_no_exception_localization_available"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>excMessage</li>
         * </ls>
         */
        TELEGRAM_INTERNAL("error_telegram_internal"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>excMessage</li>
         *  <li>excClassName</li>
         * </ls>
         */
        UNSPECIFIED_EXCEPTION("error_unspecified_exception"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>excMessage</li>
         *  <li>excClassName</li>
         *  <li>userId</li>
         *  <li>botId</li>
         * </ls>
         */
        CRITICAL_DIRECTOR_NOTIFICATION("error_critical_director_notification"),
        COURSE_NOT_FOUND("error_course_not_found"),
        LESSON_NOT_FOUND("error_lesson_not_found"),
        HOMEWORK_NOT_FOUND("error_homework_not_found"),
        CONTENT_NOT_FOUND("error_content_not_found"),
        COURSE_PROGRESS_NOT_FOUND("error_course_progress_not_found"),
        HOMEWORK_PROGRESS_NOT_FOUND("error_homework_progress_not_found"),
        USER_NOT_FOUND("error_user_not_found"),
        CONTENT_MAPPING_NOT_FOUND("error_content_mapping_not_found"),
        REVIEW_NOT_FOUND("error_review_not_found"),
        BOT_ROLE_NOT_FOUND("error_bot_role_not_found"),
        SUPPORT_REPLY_NOT_FOUND("error_support_reply_not_found"),
        SUPPORT_REQUEST_NOT_FOUND("error_support_request_not_found"),
        BOT_VISIBILITY_MISMATCH("error_bot_visibility_mismatch"),
        UNAVAILABLE_IN_REGULAR_BOT("error_unavailable_in_regular_bot"),
        COURSE_OWNERSHIP_NOT_FOUND("error_course_ownership_not_found"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>sentContentMediaType</li>
         *  <li>allowedMediaTypes</li>
         * </ls>
         */
        CONTENT_MEDIA_GROUP_DOES_NOT_MATCH("error_content_media_group_does_not_match"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>missingAuthorities</li>
         * </ls>
         */
        ACCESS_DENIED("error_access_denied"),
        CONTENT_TEXT_AND_CAPTIONS("error_content_text_and_captions"),
        CONTENT_SEVERAL_TEXT("error_content_several_text"),
        CONTENT_AUDIO_GROUP_FAILURE("error_content_audio_group_failure"),
        CONTENT_DOCUMENT_GROUP_FAILURE("error_content_document_group_failure"),
        CONTENT_GRAPHICS_GROUP_FAILURE("error_content_graphics_group_failure"),
        COURSE_UNDER_MAINTENANCE("error_course_under_maintenance"),
        UNKNOWN_MEDIA_TYPE("error_unknown_media_type"),
        MAPPING_NOT_IN_LESSON("error_mapping_not_in_lesson"),
        UPDATE_MESSAGE_FAILURE("error_update_message_failure"),
        MENU_NOT_FOUND("error_menu_not_found"),
        MENU_BUTTON_ISSUE("error_menu_button_issue"),
        MULTIPAGE_LIST_META_NOT_FOUND("error_multipage_list_meta_not_found"),
        NO_DATA_FOR_MULTIPAGE_LIST("error_no_data_for_multipage_list"),
        MTG_NOT_FOUND("error_mtg_not_found"),
        USER_IS_NOT_BANNED("error_user_is_not_banned"),
        USER_ALREADY_BANNED("error_user_already_banned"),
        CANNOT_SET_BANNED_ROLE("error_cannot_set_banned_role"),
        CREATOR_BAN("error_creator_ban"),
        SELF_BAN("error_self_ban"),
        SAME_ROLE("error_same_role"),
        PREDEFINED_CHANGE_ROLES("error_predefined_change_roles"),
        SELF_CHANGE_ROLE("error_self_change_role"),
        CREATOR_CHANGE_ROLE("error_creator_change_role"),
        DIRECTOR_CHANGE_ROLE("error_director_change_role"),
        FILE_NOT_LOCALIZATION("error_file_not_localization"),
        SEND_FILE_FAILURE("error_send_file_failure"),
        COMMIT_BASIC_REVIEW_FAILURE("error_commit_basic_review_failure"),
        REVIEW_ALREADY_PRESENT("error_review_already_present"),
        NO_ARCHIVE_REVIEWS_AVAILABLE("error_no_archive_reviews_available"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>timeLeft</li>
         * </ls>
         */
        AWAITING_LESSON("error_awaiting_lesson"),
        NO_CONTENT_IN_LESSON("error_no_content_in_lesson"),
        SELECT_LESSON_COURSE_NOT_COMPLETED("error_select_lesson_course_not_completed"),
        HOMEWORK_ALREADY_COMPLETED("error_homework_already_completed"),
        HOMEWORK_ALREADY_AWAITS_APPROVAL("error_homework_already_awaits_approval"),
        NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER("error_no_support_requests_available_for_user"),
        DIRECTOR_BAN("error_director_ban");
        
        private String locName;

        Error(String locName) {
            this.locName = locName;
        }

        @Override
        public String getLocName() {
            return locName;
        }

        public static Error findKeyByName(String name) {
            if (name == null || name.isBlank()) throw new RuntimeException("Name must not be null or blank.");

            for (final Error error : values()) {
                if (error.toString().equals(name)) return error;
            }
            throw new RuntimeException("Unable to find error localization \"" + name + "\".");
        }

        public static record MessageTextMissingParams(int messageIndex) {}
        public static record NumberOfMessagesParams(int providedMessagesNumber, int expectedMessagesNumber) {}
        public static record NoExceptionLocalizationAvailableParams(String excMessage, String excClassName) {}
        public static record TelegramInternalParams(String excMessage) {}
        public static record UnspecifiedExceptionParams(String excMessage, String excClassName) {}
        public static record CriticalDirectorNotificationParams(String excMessage, String excClassName, Long userId, Long botId) {}
        public static record ContentMediaGroupDoesNotMatchParams(MediaType sentContentMediaType, List<MediaType> allowedMediaTypes) {}
        public static record AccessDeniedParams(List<AuthorityType> missingAuthorities) {}
        public static record AwaitingLessonParams(String timeLeft) {}
    }
}

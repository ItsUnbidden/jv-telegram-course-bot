package com.unbidden.telegramcoursesbot.service.payment;

import com.unbidden.telegramcoursesbot.exception.CourseIsAlreadyOwnedException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.RefundImpossibleException;
import com.unbidden.telegramcoursesbot.exception.StaleStateException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.CurrencyCode;
import com.unbidden.telegramcoursesbot.model.TelegramPaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipSource;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipStatus;
import com.unbidden.telegramcoursesbot.repository.CourseOwnershipRepository;
import com.unbidden.telegramcoursesbot.repository.PaymentDetailsRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentService.class);
    
    private static final String PARAM_MAX_STAGE_FOR_REFUND = "${maxStageForRefund}";
    private static final String PARAM_CURRENT_STAGE = "${currentStage}";
    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_TITLE = "${targetTitle}";
    private static final String PARAM_TARGET_FULL_NAME = "${targetFullName}";
    
    private static final String ERROR_GIVE_COURSE_ALREADY_OWNED = "error_give_course_already_owned";
    private static final String ERROR_REFUND_USER_ADVANCED_TOO_FAR = "error_refund_user_advanced_too_far";
    private static final String ERROR_REFUND_COURSE_NOT_OWNED = "error_refund_course_not_owned";
    private static final String ERROR_REFUND_COURSE_WAS_GIFTED = "error_refund_course_was_gifted";
    private static final String ERROR_REFUND_COURSE_COMPLETED = "error_refund_course_completed";
    private static final String ERROR_REFUND_COURSE_UNAVAILABLE = "error_refund_course_unavailable";
    private static final String ERROR_REFUND_PURCHASE_TOO_OLD = "error_refund_purchase_too_old";

    private static final int REFUND_EXPIRATION_DAYS = 21;

    private final PaymentDetailsRepository paymentDetailsRepository;

    private final CourseOwnershipRepository courseOwnershipRepository;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public boolean isAvailable(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return isAvailable0(user, courseId);
    }

    @Transactional(readOnly = true)
    public boolean isAvailableAndGifted(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return courseOwnershipRepository.existsByUserIdAndCourseIdAndAndStatusAndSource(user.getId(), courseId,
                OwnershipStatus.ACTIVE, OwnershipSource.GIFTED);
    }

    @Transactional
    public CourseOwnership invalidateOwnership(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = entityUtil.getCourseOwnership(user, bot, courseId);

        ownership.setLastUpdate(LocalDateTime.now());
        ownership.setStatus(OwnershipStatus.REVOKED);

        return ownership;
    }

    @Transactional(readOnly = true)
    public PreCheckoutResponse checkPreCheckoutConditions(UserEntity user, Bot bot, PreCheckoutQuery preCheckoutQuery) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(preCheckoutQuery, "preCheckoutQuery cannot be null");

        try {
            final Course course = entityUtil.getCourseById(user, bot,
                    Long.parseLong(preCheckoutQuery.getInvoicePayload()));
            
            if (isAvailable(user, course.getId())) {
                return new PreCheckoutResponse(PreCheckoutResult.ALREADY_OWNED, course);
            }
            if (!preCheckoutQuery.getCurrency().equals(CurrencyCode.XTR.toString())) {
                return new PreCheckoutResponse(PreCheckoutResult.CURRENCY_MISMATCH, course);
            } 
            if (!preCheckoutQuery.getTotalAmount().equals(course.getPrice())) {
                return new PreCheckoutResponse(PreCheckoutResult.PRICE_MISMATCH, course);
            }
            
            return new PreCheckoutResponse(PreCheckoutResult.OK, course);
        } catch (EntityNotFoundException e) {
            return new PreCheckoutResponse(PreCheckoutResult.COURSE_NOT_FOUND, null);
        }
    }

    @Transactional
    public TelegramPaymentDetails registerTelegramPurchase(UserEntity user, Bot bot, SuccessfulPayment payment)
            throws CourseIsAlreadyOwnedException {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(payment, "payment cannot be null");

        final Course course = entityUtil.getCourseById(user, bot, Long.parseLong(payment.getInvoicePayload()));
        final CourseOwnership ownership = createOrActivateOwnership(user, course, OwnershipSource.TELEGRAM);

        LOGGER.debug("Ownership " + ownership.getId() + " has been successfully created/updated. Recording payment details...");
        final TelegramPaymentDetails paymentDetails = new TelegramPaymentDetails();

        paymentDetails.setTelegramPaymentChargeId(payment.getTelegramPaymentChargeId());
        paymentDetails.setBot(bot);
        paymentDetails.setUser(user);
        paymentDetails.setCourse(course);
        paymentDetails.setTimestamp(LocalDateTime.now());
        paymentDetails.setTotalAmount(payment.getTotalAmount());

        ownership.setLastPaymentDetails(paymentDetailsRepository.save(paymentDetails));

        return paymentDetails;
    }

    @Transactional
    public CourseOwnership registerGiftCourse(UserEntity user, Bot bot, Long targetId, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(user, bot, courseId);
        final UserEntity target = entityUtil.getUser(courseId, user.getLanguageCode());

        try {
            return createOrActivateOwnership(target, course, OwnershipSource.GIFTED);
        } catch (CourseIsAlreadyOwnedException e) {
            final Map<String, Object> paramMap = new HashMap<>();

            paramMap.put(PARAM_COURSE_NAME, contentService.getLocalizedText(user, bot, course.getTitle().getId()));
            paramMap.put(PARAM_TARGET_FULL_NAME, target.getFullName());
            paramMap.put(PARAM_TITLE, entityUtil.getLocalizedTitle(user, bot, target));
            throw new StaleStateException("Unable to gift course " + courseId + " to user "
                    + targetId + " because they already own it.", localizationLoader.getLocalizationForUser(
                    ERROR_GIVE_COURSE_ALREADY_OWNED, user, paramMap), e);
        }
    }

    @Transactional(readOnly = true)
    public CourseOwnership checkRefundPossible(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(user, bot, courseId);
        final String courseName = contentService.getLocalizedText(user, bot, courseId);

        LOGGER.info("Performing checks for refund of course " + courseId
                + " for user " + user.getId() + "...");
        LOGGER.debug("Checking whether course " + courseId + " supports refund...");
        if (course.getRefundStage() < 0) {
            throw new RefundImpossibleException("Refund for course " + courseId 
                    + " is not possible", localizationLoader.getLocalizationForUser(
                    ERROR_REFUND_COURSE_UNAVAILABLE, user, PARAM_COURSE_NAME,
                    courseName));
        }
        LOGGER.debug("Checking whether course " + courseId + " is owned by user " + user.getId() + "...");
        if (!isAvailable(user, courseId)) {
            throw new RefundImpossibleException("Course " + courseId
                    + " is not owned by user " + user.getId(), localizationLoader
                    .getLocalizationForUser(ERROR_REFUND_COURSE_NOT_OWNED, user));
        }
        LOGGER.debug("Checking whether course " + courseId + " was gifted to user "
                + user.getId() + "...");
        if (isAvailableAndGifted(user, courseId)) {
            throw new RefundImpossibleException("Course " + courseId
                    + " was gifted to user " + user.getId()
                    + " and therefore it cannot be refunded", localizationLoader
                    .getLocalizationForUser(ERROR_REFUND_COURSE_WAS_GIFTED, user));
        }
        final CourseOwnership ownership = entityUtil.getCourseOwnership(user, bot, courseId);

        LOGGER.debug("Checking whether the refund expiration period is over for user "
                + user.getId() + "'s ownership for course " + courseId + "...");
        if (ownership.getLastUpdate().plusDays(REFUND_EXPIRATION_DAYS)
                .isBefore(LocalDateTime.now())) {
            throw new RefundImpossibleException("User " + user.getId() + " cannot refund course "
                    + courseId + " because " + REFUND_EXPIRATION_DAYS + " days have passed "
                    + "since the purchase.", localizationLoader.getLocalizationForUser(
                    ERROR_REFUND_PURCHASE_TOO_OLD, user));
        }
        final CourseProgress courseProgress = entityUtil.getCourseProgressForUser(user, bot, courseId);

        LOGGER.debug("Checking whether course " + courseId + " has been completed by user "
                + user.getId() + "..."); 
        if (courseProgress.getNumberOfTimesCompleted() > 0) {
            throw new RefundImpossibleException("User " + user.getId() + " cannot refund course "
                    + courseId + " because they have already completed it",
                    localizationLoader.getLocalizationForUser(ERROR_REFUND_COURSE_COMPLETED,
                    user, PARAM_COURSE_NAME, courseName));
        }
        LOGGER.debug("Checking whether user " + user.getId() + " has advanced past stage "
                + course.getRefundStage() + " in course " + courseId + " (current stage is "
                + courseProgress.getStage() + ")...");
        if (courseProgress.getStage() > course.getRefundStage()) {
            final Map<String, Object> parameterMap = new HashMap<>();

            parameterMap.put(PARAM_COURSE_NAME, courseName);
            parameterMap.put(PARAM_CURRENT_STAGE, courseProgress.getStage());
            parameterMap.put(PARAM_MAX_STAGE_FOR_REFUND, course.getRefundStage());

            throw new RefundImpossibleException("User " + user.getId()
                    + " has advanced in course " + courseId + " to "
                    + courseProgress.getStage() + " lesson which is past lesson "
                    + course.getRefundStage() + " and therefore refund is now impossible",
                    localizationLoader.getLocalizationForUser(ERROR_REFUND_USER_ADVANCED_TOO_FAR,
                    user, parameterMap));
        }
        LOGGER.info("User " + user.getId() + " is eligible for course "
                + courseId + "'s refund.");
        return ownership;
    }

    @Transactional
    public CourseOwnership invalidateTelegramCourseOwnership(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = invalidateOwnership(user, bot, courseId);
        final TelegramPaymentDetails paymentDetails = (TelegramPaymentDetails)ownership.getLastPaymentDetails();

        paymentDetails.setRefundedAt(LocalDateTime.now());

        return ownership;
    }

    private boolean isAvailable0(UserEntity user, Long courseId) {
        return courseOwnershipRepository.existsByUserIdAndCourseIdAndAndStatus(user.getId(),
                courseId, OwnershipStatus.ACTIVE);
    }

    private CourseOwnership createOrActivateOwnership(UserEntity user, Course course, OwnershipSource source)
            throws CourseIsAlreadyOwnedException {
        final Optional<CourseOwnership> ownershipOpt = courseOwnershipRepository
                .findByUserIdAndCourseId(user.getId(), course.getId());

        final CourseOwnership ownership;
        if (ownershipOpt.isPresent() && ownershipOpt.get().getStatus() == OwnershipStatus.REVOKED) {
            LOGGER.debug("User " + user.getId() + " already has a deactivated course ownership for course "
                    + course.getId() + ". It will be reactivated.");
            ownership = ownershipOpt.get();
        } else {
            LOGGER.debug("User " + user.getId() + " does not have a deactivated course ownership for course "
                        + course.getId() + ". A new one will be created.");
            ownership = new CourseOwnership();
    
            ownership.setCourse(course);
            ownership.setUser(user);
        }
        ownership.setLastUpdate(LocalDateTime.now());
        ownership.setSource(source);
        ownership.setStatus(OwnershipStatus.ACTIVE);

        try {
            return courseOwnershipRepository.saveAndFlush(ownership);
        } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
            throw new CourseIsAlreadyOwnedException("Cannot register a new ownership for user " + user.getId()
                    + " and course " + course.getId() + ", because it already exists.", e);
        }
    }

    public static record PreCheckoutResponse(PreCheckoutResult result, Course course) {}

    public static enum PreCheckoutResult {
        OK,
        ALREADY_OWNED,
        CURRENCY_MISMATCH,
        PRICE_MISMATCH,
        COURSE_NOT_FOUND
    }
}

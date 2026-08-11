package com.unbidden.telegramcoursesbot.service.payment;

import com.unbidden.telegramcoursesbot.exception.CourseIsAlreadyOwnedException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.RefundImpossibleException;
import com.unbidden.telegramcoursesbot.exception.StaleStateException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    
    private static final int REFUND_EXPIRATION_DAYS = 21;

    private final PaymentDetailsRepository paymentDetailsRepository;

    private final CourseOwnershipRepository courseOwnershipRepository;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public List<CourseOwnership> getActiveOwnershipsForUserInBot(UserEntity user, Bot bot) {
        return courseOwnershipRepository.findByUserIdAndCourseBotIdAndStatus(user.getId(), bot.getId(), OwnershipStatus.ACTIVE);
    }

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
            throw new StaleStateException("Unable to gift course " + courseId + " to user "
                    + targetId + " because they already own it.", localizationLoader.localize(
                    Error.GIVE_COURSE_ALREADY_OWNED, user, new Error.GiveCourseAlreadyOwnedParams(
                        contentService.getLocalizedText(user, bot, course.getTitle().getId()), target.getFullName(),
                        entityUtil.getLocalizedTitle(user, bot, target))), e);
        }
    }

    @Transactional(readOnly = true)
    public CourseOwnership checkRefundPossible(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return checkRefundPossible0(user, bot, courseId, null, null, null);
    }

    @Transactional(readOnly = true)
    public CourseOwnership checkRefundPossible(UserEntity user, Bot bot, Course course,
            CourseOwnership ownership, CourseProgress progress) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(course, "course cannot be null");
        Assert.notNull(ownership, "ownership cannot be null");
        Assert.notNull(progress, "progress cannot be null");
        
        return checkRefundPossible0(user, bot, course.getId(), course, ownership, progress);
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

    private CourseOwnership checkRefundPossible0(UserEntity user, Bot bot, Long courseId, Course course,
            CourseOwnership ownership, CourseProgress progress) {
        try {
            if (course == null) {
                course = entityUtil.getCourseById(user, bot, courseId);
            }
            final String courseName = contentService.getLocalizedText(user, bot, course.getTitle());

            LOGGER.info("Performing checks for refund of course " + courseId
                    + " for user " + user.getId() + "...");
            LOGGER.debug("Checking whether course " + courseId + " supports refund...");
            if (course.getRefundStage() < 0) {
                throw new RefundImpossibleException("Refund for course " + courseId 
                        + " is not possible", localizationLoader.localize(
                        Error.REFUND_COURSE_UNAVAILABLE, user, new Error.RefundCourseUnavailableParams(courseName)));
            }
            LOGGER.debug("Checking whether course " + courseId + " is owned by user " + user.getId() + "...");
            if (!isAvailable(user, courseId)) {
                throw new RefundImpossibleException("Course " + courseId
                        + " is not owned by user " + user.getId(), localizationLoader
                        .localize(Error.REFUND_COURSE_NOT_OWNED, user, new Error.RefundCourseNotOwnedParams(courseName)));
            }
            LOGGER.debug("Checking whether course " + courseId + " was gifted to user "
                    + user.getId() + "...");
            if (isAvailableAndGifted(user, courseId)) {
                throw new RefundImpossibleException("Course " + courseId
                        + " was gifted to user " + user.getId()
                        + " and therefore it cannot be refunded", localizationLoader
                        .localize(Error.REFUND_COURSE_WAS_GIFTED, user, new Error.RefundCourseWasGiftedParams(courseName)));
            }

            if (ownership == null) {
                ownership = entityUtil.getCourseOwnership(user, bot, courseId);
            }
            LOGGER.debug("Checking whether the refund expiration period is over for user "
                    + user.getId() + "'s ownership for course " + courseId + "...");
            if (ownership.getLastUpdate().plusDays(REFUND_EXPIRATION_DAYS)
                    .isBefore(LocalDateTime.now())) {
                throw new RefundImpossibleException("User " + user.getId() + " cannot refund course "
                        + courseId + " because more than " + REFUND_EXPIRATION_DAYS + " days have passed "
                        + "since the purchase.", localizationLoader.localize(
                        Error.REFUND_PURCHASE_TOO_OLD, user, new Error.RefundPurchaseTooOldParams(courseName, REFUND_EXPIRATION_DAYS,
                            ChronoUnit.DAYS.between(ownership.getLastUpdate(), LocalDateTime.now()))));
            }
            if (progress == null) {
                try {
                    progress = entityUtil.getCourseProgressForUser(user, bot, courseId);
                } catch (EntityNotFoundException e) {
                    LOGGER.info("User " + user.getId() + " is eligible for a refund of course "
                            + courseId + " because they haven't started it yet.");
                    return ownership;
                }
            }
            
            LOGGER.debug("Checking whether course " + courseId + " has been completed by user "
                    + user.getId() + "..."); 
            if (progress.getNumberOfTimesCompleted() > 0) {
                throw new RefundImpossibleException("User " + user.getId() + " cannot refund course "
                        + courseId + " because they have already completed it",
                        localizationLoader.localize(Error.REFUND_COURSE_COMPLETED,
                        user, new Error.RefundCourseCompletedParams(courseName)));
            }
            LOGGER.debug("Checking whether user " + user.getId() + " has advanced past stage "
                    + course.getRefundStage() + " in course " + courseId + " (current stage is "
                    + progress.getStage() + ")...");
            if (progress.getStage() > course.getRefundStage()) {
                throw new RefundImpossibleException("User " + user.getId()
                        + " has advanced in course " + courseId + " to "
                        + progress.getStage() + " lesson which is past lesson "
                        + course.getRefundStage() + " and therefore the refund is now impossible",
                        localizationLoader.localize(Error.REFUND_USER_ADVANCED_TOO_FAR,
                            user, new Error.RefundUserAdvancedTooFarParams(courseName, course.getRefundStage(),
                            progress.getStage())));
            }
            LOGGER.info("User " + user.getId() + " is eligible for course "
                    + courseId + "'s refund.");

            return ownership;
        } catch (EntityNotFoundException e) {
            throw new RefundImpossibleException("Some entity was not found during a refund check. "
                    + "This might be due to a stale request.", localizationLoader.localize(Error.REFUND_ENTITY_NOT_FOUND, user));
        }
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

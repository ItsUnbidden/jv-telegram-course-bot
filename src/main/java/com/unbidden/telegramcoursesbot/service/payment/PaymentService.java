package com.unbidden.telegramcoursesbot.service.payment;

import com.unbidden.telegramcoursesbot.exception.CourseBoughtException;
import com.unbidden.telegramcoursesbot.exception.CourseIsAlreadyOwnedException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.RefundImpossibleException;
import com.unbidden.telegramcoursesbot.exception.StaleStateException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.CurrencyCode;
import com.unbidden.telegramcoursesbot.model.TelegramInvoice;
import com.unbidden.telegramcoursesbot.model.TelegramPaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipSource;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipStatus;
import com.unbidden.telegramcoursesbot.repository.CourseOwnershipRepository;
import com.unbidden.telegramcoursesbot.repository.PaymentDetailsRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
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

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public List<CourseOwnership> getActiveOwnershipsForUserInBot(BotRole botRole) {
        return courseOwnershipRepository.findByUserIdAndCourseBotIdAndStatus(botRole.getUser().getId(),
                botRole.getBot().getId(), OwnershipStatus.ACTIVE);
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
    public CourseOwnership invalidateOwnership(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = entityUtil.getCourseOwnership(botRole, courseId);

        ownership.setLastUpdate(LocalDateTime.now());
        ownership.setStatus(OwnershipStatus.REVOKED);

        return ownership;
    }

    @Transactional(readOnly = true)
    public PreCheckoutResponse checkPreCheckoutConditions(BotRole botRole, PreCheckoutQuery preCheckoutQuery) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(preCheckoutQuery, "preCheckoutQuery cannot be null");

        try {
            final Course course = entityUtil.getCourseById(botRole, Long.parseLong(preCheckoutQuery.getInvoicePayload()));
            
            if (isAvailable(botRole.getUser(), course.getId())) {
                return new PreCheckoutResponse(PreCheckoutResult.ALREADY_OWNED, course);
            }
            if (!course.getInvoice().getClass().equals(TelegramInvoice.class)) {
                return new PreCheckoutResponse(PreCheckoutResult.INVALID_INVOICE, course);
            }
            if (!preCheckoutQuery.getCurrency().equals(CurrencyCode.XTR.toString())) {
                return new PreCheckoutResponse(PreCheckoutResult.CURRENCY_MISMATCH, course);
            } 
            if (!preCheckoutQuery.getTotalAmount().equals(((TelegramInvoice)course.getInvoice()).getPrice())) {
                return new PreCheckoutResponse(PreCheckoutResult.PRICE_MISMATCH, course);
            }
            
            return new PreCheckoutResponse(PreCheckoutResult.OK, course);
        } catch (EntityNotFoundException e) {
            return new PreCheckoutResponse(PreCheckoutResult.COURSE_NOT_FOUND, null);
        }
    }

    @Transactional
    public TelegramPaymentDetails registerTelegramPurchase(BotRole botRole, SuccessfulPayment payment)
            throws CourseIsAlreadyOwnedException {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(payment, "payment cannot be null");

        final Course course = entityUtil.getCourseById(botRole, Long.parseLong(payment.getInvoicePayload()));
        final CourseOwnership ownership = createOrActivateOwnership(botRole, course, OwnershipSource.TELEGRAM);

        LOGGER.debug("Ownership " + ownership.getId() + " has been successfully created/updated. Recording payment details...");
        final TelegramPaymentDetails paymentDetails = new TelegramPaymentDetails();

        paymentDetails.setTelegramPaymentChargeId(payment.getTelegramPaymentChargeId());
        paymentDetails.setBot(entityUtil.getBotReference(botRole.getBot().getId()));
        paymentDetails.setUser(entityUtil.getUserReference(botRole.getUser().getId()));
        paymentDetails.setCourse(course);
        paymentDetails.setTimestamp(LocalDateTime.now());
        paymentDetails.setTotalAmount(payment.getTotalAmount());

        ownership.setLastPaymentDetails(paymentDetailsRepository.save(paymentDetails));

        return paymentDetails;
    }

    @Transactional
    public CourseOwnership registerGiftCourse(BotRole botRole, Long targetId, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(botRole, courseId);
        final BotRole targetBotRole = entityUtil.getActiveBotRole(botRole, targetId);

        try {
            return createOrActivateOwnership(targetBotRole, course, OwnershipSource.GIFTED);
        } catch (CourseIsAlreadyOwnedException e) {
            throw new StaleStateException("Unable to gift course " + courseId + " to user "
                    + targetId + " because they already own it.", localizationLoader.localize(
                    Error.GIVE_COURSE_ALREADY_OWNED, botRole, new Error.GiveCourseAlreadyOwnedParams(
                        contentService.getLocalizedText(botRole, course.getTitle().getId()), targetBotRole.getUser().getFullName(),
                        entityUtil.getLocalizedTitle(botRole, targetBotRole))), e);
        }
    }

    @Transactional
    public CourseOwnership invalidateGiftCourse(BotRole botRole, Long targetId, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final BotRole targetBotRole = entityUtil.getActiveBotRole(botRole, targetId);
        final CourseOwnership ownership = entityUtil.getActiveCourseOwnership(targetBotRole, courseId);

        if (ownership.getSource() != OwnershipSource.GIFTED) {
            throw new CourseBoughtException("User " + targetId + " has bought course " + courseId + ". It cannot be taken away.",
                    localizationLoader.localize(Localizations.Error.TAKE_COURSE_BOUGHT, botRole, new Localizations.Error.TakeCourseBoughtParams(
                        contentService.getLocalizedText(botRole, ownership.getCourse().getTitle().getId()), targetBotRole.getUser().getFullName(),
                        entityUtil.getLocalizedTitle(botRole, targetBotRole))));
        }

        ownership.setLastUpdate(LocalDateTime.now());
        ownership.setStatus(OwnershipStatus.REVOKED);

        return ownership;
    }

    @Transactional(readOnly = true)
    public CourseOwnership checkRefundPossible(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        try {
            return checkRefundPossible0(botRole, courseId, null, null, null);
        } catch (RefundImpossibleException e) {
            throw new ForbiddenOperationException("Refund for course " + courseId + " is impossible for user "
                    + botRole.getUser().getId() + ".", e.getLoc(), e);
        }
    }

    @Transactional(readOnly = true)
    public CourseOwnership checkRefundPossible(BotRole botRole, Course course,
            CourseOwnership ownership, CourseProgress progress) throws RefundImpossibleException {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(course, "course cannot be null");
        Assert.notNull(ownership, "ownership cannot be null");
        Assert.notNull(progress, "progress cannot be null");
        
        return checkRefundPossible0(botRole, course.getId(), course, ownership, progress);
    }

    @Transactional
    public CourseOwnership invalidateTelegramCourseOwnership(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = invalidateOwnership(botRole, courseId);
        final TelegramPaymentDetails paymentDetails = (TelegramPaymentDetails)ownership.getLastPaymentDetails();

        paymentDetails.setRefundedAt(LocalDateTime.now());

        return ownership;
    }

    private CourseOwnership checkRefundPossible0(BotRole botRole, Long courseId, Course course,
            CourseOwnership ownership, CourseProgress progress) throws RefundImpossibleException {
        try {
            if (course == null) {
                course = entityUtil.getCourseById(botRole, courseId);
            }
            final String courseName = contentService.getLocalizedText(botRole, course.getTitle());

            if (!course.getInvoice().getClass().equals(TelegramInvoice.class)) {
                throw new RefundImpossibleException("Course " + course.getId() + " does not have a Telegram invoice.",
                        localizationLoader.localize(Error.REFUND_COURSE_UNAVAILABLE, botRole, new Error.RefundCourseUnavailableParams(courseName)));
            }
            final TelegramInvoice invoice = (TelegramInvoice)course.getInvoice();

            LOGGER.info("Performing checks for refund of course " + courseId
                    + " for user " + botRole.getUser().getId() + "...");
            LOGGER.debug("Checking whether course " + courseId + " supports refund...");
            if (invoice.getRefundStage() == null) {
                throw new RefundImpossibleException("Refund for course " + courseId 
                        + " is not possible", localizationLoader.localize(
                        Error.REFUND_COURSE_UNAVAILABLE, botRole, new Error.RefundCourseUnavailableParams(courseName)));
            }
            LOGGER.debug("Checking whether course " + courseId + " is owned by user " + botRole.getUser().getId() + "...");
            if (!isAvailable(botRole.getUser(), courseId)) {
                throw new RefundImpossibleException("Course " + courseId
                        + " is not owned by user " + botRole.getUser().getId(), localizationLoader
                        .localize(Error.REFUND_COURSE_NOT_OWNED, botRole, new Error.RefundCourseNotOwnedParams(courseName)));
            }

            if (ownership == null) {
                ownership = entityUtil.getCourseOwnership(botRole, courseId);
            }
            LOGGER.debug("Checking whether the course was aquired through a Telegram purchase...");
            if (ownership.getSource() != OwnershipSource.TELEGRAM) {
                throw new RefundImpossibleException("User " + botRole.getUser().getId() + " cannot refund course "
                        + course.getId() + " because they did not buy it through Telegram.",
                        localizationLoader.localize(Localizations.Error.REFUND_INVALID_SOURCE, botRole,
                            new Localizations.Error.RefundInvalidSourceParams(courseName)));
            }
            LOGGER.debug("Checking whether the refund expiration period is over for user "
                    + botRole.getUser().getId() + "'s ownership for course " + courseId + "...");
            if (ownership.getLastUpdate().plusDays(REFUND_EXPIRATION_DAYS)
                    .isBefore(LocalDateTime.now())) {
                throw new RefundImpossibleException("User " + botRole.getUser().getId() + " cannot refund course "
                        + courseId + " because more than " + REFUND_EXPIRATION_DAYS + " days have passed "
                        + "since the purchase.", localizationLoader.localize(
                        Error.REFUND_PURCHASE_TOO_OLD, botRole, new Error.RefundPurchaseTooOldParams(courseName, REFUND_EXPIRATION_DAYS,
                            ChronoUnit.DAYS.between(ownership.getLastUpdate(), LocalDateTime.now()))));
            }
            if (progress == null) {
                try {
                    progress = entityUtil.getCourseProgressForUser(botRole, courseId);
                } catch (EntityNotFoundException e) {
                    LOGGER.info("User " + botRole.getUser().getId() + " is eligible for a refund of course "
                            + courseId + " because they haven't started it yet.");
                    return ownership;
                }
            }
            
            LOGGER.debug("Checking whether course " + courseId + " has been completed by user "
                    + botRole.getUser().getId() + "..."); 
            if (progress.getNumberOfTimesCompleted() > 0) {
                throw new RefundImpossibleException("User " + botRole.getUser().getId() + " cannot refund course "
                        + courseId + " because they have already completed it",
                        localizationLoader.localize(Error.REFUND_COURSE_COMPLETED,
                        botRole, new Error.RefundCourseCompletedParams(courseName)));
            }
            LOGGER.debug("Checking whether user " + botRole.getUser().getId() + " has advanced past stage "
                    + invoice.getRefundStage() + " in course " + courseId + " (current stage is "
                    + progress.getStage() + ")...");
            if (progress.getStage() > invoice.getRefundStage()) {
                throw new RefundImpossibleException("User " + botRole.getUser().getId()
                        + " has advanced in course " + courseId + " to "
                        + progress.getStage() + " lesson which is past lesson "
                        + invoice.getRefundStage() + " and therefore the refund is now impossible",
                        localizationLoader.localize(Error.REFUND_USER_ADVANCED_TOO_FAR,
                            botRole, new Error.RefundUserAdvancedTooFarParams(courseName, invoice.getRefundStage(),
                            progress.getStage())));
            }
            LOGGER.info("User " + botRole.getUser().getId() + " is eligible for course "
                    + courseId + "'s refund.");

            return ownership;
        } catch (EntityNotFoundException e) {
            throw new RefundImpossibleException("Some entity was not found during a refund check. "
                    + "This might be due to a stale request.", localizationLoader.localize(Error.REFUND_ENTITY_NOT_FOUND, botRole));
        }
    }

    private boolean isAvailable0(UserEntity user, Long courseId) {
        return courseOwnershipRepository.existsByUserIdAndCourseIdAndAndStatus(user.getId(),
                courseId, OwnershipStatus.ACTIVE);
    }

    private CourseOwnership createOrActivateOwnership(BotRole botRole, Course course, OwnershipSource source)
            throws CourseIsAlreadyOwnedException {
        final Optional<CourseOwnership> ownershipOpt = courseOwnershipRepository
                .findByUserIdAndCourseId(botRole.getUser().getId(), course.getId());

        final CourseOwnership ownership;
        if (ownershipOpt.isPresent() && ownershipOpt.get().getStatus() == OwnershipStatus.REVOKED) {
            LOGGER.debug("User " + botRole.getUser().getId() + " already has a deactivated course ownership for course "
                    + course.getId() + ". It will be reactivated.");
            ownership = ownershipOpt.get();
        } else {
            LOGGER.debug("User " + botRole.getUser().getId() + " does not have a deactivated course ownership for course "
                        + course.getId() + ". A new one will be created.");
            ownership = new CourseOwnership();
    
            ownership.setCourse(course);
            ownership.setUser(entityUtil.getUser(botRole, botRole.getUser().getId()));
        }
        ownership.setLastUpdate(LocalDateTime.now());
        ownership.setSource(source);
        ownership.setStatus(OwnershipStatus.ACTIVE);

        try {
            return courseOwnershipRepository.saveAndFlush(ownership);
        } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
            throw new CourseIsAlreadyOwnedException("Cannot register a new ownership for user " + botRole.getUser().getId()
                    + " and course " + course.getId() + ", because it already exists.", e);
        }
    }

    public static record PreCheckoutResponse(PreCheckoutResult result, Course course) {}

    public static enum PreCheckoutResult {
        OK,
        INVALID_INVOICE,
        ALREADY_OWNED,
        CURRENCY_MISMATCH,
        PRICE_MISMATCH,
        COURSE_NOT_FOUND
    }
}

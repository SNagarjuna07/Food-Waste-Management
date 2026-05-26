package com.abc.foodwastemanagement.scheduler;

import com.abc.foodwastemanagement.repository.EmailVerificationRepository;
import com.abc.foodwastemanagement.repository.NotificationRepository;
import com.abc.foodwastemanagement.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final EmailVerificationRepository emailVerificationRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final NotificationRepository notificationRepository;


    // Weekly cleanup for email verification tokens
    @Transactional
    @Scheduled(cron = "0 00 18 ? * SUN")
    public void cleanupEmailVerificationTokens() {

        log.info("Starting cleanup: Email verification tokens");

        emailVerificationRepository.deleteExpiredOrUsed(LocalDateTime.now());

        log.info("Completed cleanup: Email verification tokens");
    }

    // Weekly cleanup for password reset tokens
    @Transactional
    @Scheduled(cron = "0 00 19 ? * SUN")
    public void cleanupPasswordResetTokens() {

        log.info("Starting cleanup: Password reset tokens");

        passwordResetTokenRepository.deleteExpiredOrUsed(LocalDateTime.now());

        log.info("Completed cleanup: Password reset tokens");
    }

    // Daily cleanup for read notifications older than retention period
    @Transactional
    @Scheduled(cron = "0 00 12 ? * *")
    public void cleanupNotifications() {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(10);

        log.info("Starting cleanup: Notifications read before {}", cutoff);

        notificationRepository.deleteByReadTrueAndCreatedAtBefore(cutoff);

        log.info("Completed cleanup: Notifications");
    }
}

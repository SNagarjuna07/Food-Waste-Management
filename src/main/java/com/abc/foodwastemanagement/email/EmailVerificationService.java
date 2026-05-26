package com.abc.foodwastemanagement.email;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abc.foodwastemanagement.entity.EmailVerification;
import com.abc.foodwastemanagement.entity.User;
import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.enums.KafkaNotificationType;
import com.abc.foodwastemanagement.enums.NotificationType;
import com.abc.foodwastemanagement.exception.InvalidRequestException;
import com.abc.foodwastemanagement.kafka.NotificationProducer;
import com.abc.foodwastemanagement.notification.NotificationService;
import com.abc.foodwastemanagement.repository.EmailVerificationRepository;
import com.abc.foodwastemanagement.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;

    private final UserRepository userRepository;

    private final NotificationService notificationService;

    private final NotificationProducer notificationProducer;

    @Transactional
    public void verifyEmail(String tokenValue) {

        EmailVerification token = emailVerificationRepository.findByToken(tokenValue)
            .orElseThrow(() ->
                new InvalidRequestException(
                    ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN,
                    "Invalid verification token."
                )
            );

        if (token.isUsed()) {
            throw new InvalidRequestException(
                ErrorCode.EMAIL_VERIFICATION_TOKEN_USED,
                "Token already used."
            );
        }

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException(
                ErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED,
                "Token has been expired."
            );
        }

        User user = userRepository.findById(token.getUserId())
            .orElseThrow(() ->
                new InvalidRequestException(
                    ErrorCode.USER_NOT_FOUND,
                    "User not found."
                )
            );

        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        emailVerificationRepository.save(token);

        // Notification for that user
        notificationService.saveNotification(
            user.getId(),
            "Email verification",
            "Your email has been verified successfully.",
            NotificationType.INFO
        );

        // Kafka producer
        notificationProducer.publish(
            user.getId().toString(),
            KafkaNotificationType.EMAIL_VERIFIED,
            "Email Verified",
            "Your email address has been successfully verified."
        );
        
    }
}

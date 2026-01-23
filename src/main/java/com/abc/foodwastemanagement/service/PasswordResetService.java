package com.abc.foodwastemanagement.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abc.foodwastemanagement.email.EmailService;
import com.abc.foodwastemanagement.entity.PasswordResetToken;
import com.abc.foodwastemanagement.entity.User;
import com.abc.foodwastemanagement.enums.AuthProvider;
import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.enums.KafkaNotificationType;
import com.abc.foodwastemanagement.enums.NotificationType;
import com.abc.foodwastemanagement.exception.ExpiredPasswordResetTokenException;
import com.abc.foodwastemanagement.exception.InvalidPasswordResetTokenException;
import com.abc.foodwastemanagement.exception.InvalidRequestException;
import com.abc.foodwastemanagement.exception.PasswordResetTokenAlreadyUsedException;
import com.abc.foodwastemanagement.exception.UserNotFoundException;
import com.abc.foodwastemanagement.kafka.NotificationProducer;
import com.abc.foodwastemanagement.notification.NotificationService;
import com.abc.foodwastemanagement.repository.UserRepository;
import com.abc.foodwastemanagement.repository.PasswordResetTokenRepository;


import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 15;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationProducer notificationProducer; 

    @Autowired
    private NotificationService notificationService;

    // Forgot password
    public void forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new InvalidRequestException(
                ErrorCode.EMAIL_NOT_FOUND,
                "This email does not exist."
            ));

        if(user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new InvalidRequestException(ErrorCode.EMAIL_VERIFICATION_NOT_REQUIRED, "You have logged in using your Google account.");
        }
    
        // Create password reset token
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUserId(user.getId());
        token.setExpiryTime(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        token.setUsed(false);
    
        tokenRepository.save(token);
    
        // Generate reset link
        String resetLink = "http://localhost:8080/auth/reset-password?token=" + token.getToken();
    
        // Send password reset email
        emailService.sendPasswordResetEmail(
            user.getUsername(),
            user.getEmail(),
            resetLink
        );
    
        // Save notification in DB
        notificationService.saveNotification(
            user.getId(),
            "Password reset link",
            "Password reset link sent successfully to your email.",
            NotificationType.INFO
        );
    
        // Send Kafka notification
        notificationProducer.publish(
            user.getId().toHexString(),
            KafkaNotificationType.PASSWORD_RESET_REQUESTED,
            "Password reset",
            "Password reset link has been sent to your email."
        );
    
        log.info(
            "Password reset token generated for userId={}, username={}",
            user.getId(),
            user.getUsername()
        );
    }
    

    // Reset Password
    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {

        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
            .orElseThrow(() -> new InvalidPasswordResetTokenException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN, "Invalid token"));

        if (token.isUsed()) {
            throw new PasswordResetTokenAlreadyUsedException(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED,"Token already used");
        }

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ExpiredPasswordResetTokenException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED, "Token expired");
        }

        User user = userRepository.findById(token.getUserId())
            .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

         // Save notification to DB
        notificationService.saveNotification(
            user.getId(),
            "Password change success", 
            "You account's password has been changed successfully.", 
            NotificationType.INFO 
        );

        // Kafka notification
        notificationProducer.publish(
            user.getId().toHexString(),
            KafkaNotificationType.PASSWORD_RESET_COMPLETED,
            "Password Reset Completed",
            "Password was changed successfully."
        );
        
        log.info("Password resetted for user id = {}, name = {}", user.getId(), user.getUsername());
    }

}

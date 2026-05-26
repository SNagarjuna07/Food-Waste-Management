package com.abc.foodwastemanagement.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService  {

    private final JavaMailSender mailSender;

    @Async("emailTaskExecutor")
    public void sendEmailVerificationEmail(String username, String to, String verifyLink) {

        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setTo(to);
        message.setSubject("Verify your email");
        message.setText(
            "Hello " + username + ",\n\n" +
            "Click the link below to verify your email:\n" +
            verifyLink + "\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "Regards,\nFood Waste Management Team"
        );

        try {

            mailSender.send(message);

            log.info("Sending verification email on thread {} to: {}", Thread.currentThread().getName(), to);
            
        } catch(Exception e) {
           
            log.error("Failed to send verification email to {}", to, e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String username, String to, String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText(
            "Hello " + username + ",\n\n" +
            "You requested to reset your password.\n\n" +
            "Click the link below to reset your password:\n" +
            resetLink + "\n\n" +
            "This link will expire in 15 minutes.\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "Regards,\nFood Waste Management Team"
        );

        try {

            mailSender.send(message);

            log.info("Sending password reset email on thread {} to: {}", Thread.currentThread().getName(), to);
            
        } catch(Exception e) {
           
            log.error("Failed to send password reset email to {}", to, e);
        }
    }
}

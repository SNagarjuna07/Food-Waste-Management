package com.abc.foodwastemanagement.service;

import com.abc.foodwastemanagement.constants.RoleConstants;
import com.abc.foodwastemanagement.dto.user.RegisterUserRequest;
import com.abc.foodwastemanagement.dto.user.UserIdentity;
import com.abc.foodwastemanagement.dto.user.UserUpdateDto;
import com.abc.foodwastemanagement.email.EmailService;
import com.abc.foodwastemanagement.entity.EmailVerification;
import com.abc.foodwastemanagement.entity.User;
import com.abc.foodwastemanagement.enums.AuthProvider;
import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.enums.KafkaNotificationType;
import com.abc.foodwastemanagement.enums.NotificationType;
import com.abc.foodwastemanagement.exception.*;
import com.abc.foodwastemanagement.kafka.NotificationProducer;
import com.abc.foodwastemanagement.notification.NotificationService;
import com.abc.foodwastemanagement.repository.EmailVerificationRepository;
import com.abc.foodwastemanagement.repository.UserRepository;
import com.abc.foodwastemanagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailVerificationRepository emailVerificationRepository;

    private final EmailService emailService;

    private final NotificationProducer notificationProducer;

    private final NotificationService notificationService;

    private final AuthenticatedUserService authenticatedUserService;

    // Register
    @Transactional
    public void registerUser(RegisterUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email already exists"
            );
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRoles(List.of(RoleConstants.ROLE_USER));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        EmailVerification token = new EmailVerification();
        token.setToken(UUID.randomUUID().toString());
        token.setUserId(user.getId());
        token.setExpiryTime(LocalDateTime.now().plusHours(24));
        token.setUsed(false);

        emailVerificationRepository.save(token);

        emailService.sendEmailVerificationEmail(
                user.getUsername(),
                user.getEmail(),
                "http://localhost:8080/auth/verify-email?token=" + token.getToken()
        );

        notificationService.saveNotification(
                user.getId(),
                "Email verification link",
                "Verification link sent successfully to your mail.",
                NotificationType.INFO
        );

        notificationProducer.publish(
                user.getId().toHexString(),
                KafkaNotificationType.EMAIL_VERIFIED,
                "Email verification link",
                "Verification link sent successfully to your mail."
        );
    }

    // USER IDENTITY (CACHED)

    @Transactional(readOnly = true)
    @Cacheable(value = "userByUsername", key = "#username", unless = "#result == null")
    public UserIdentity getUserIdentityByUsername(String username) {

        log.info("Fetching user identity from DB for username={}", username);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException(
                    ErrorCode.USER_NOT_FOUND,
                    "User not found"
            );
        }

        return mapToIdentity(user);
    }

    //LOGIN
    @Transactional(readOnly = true)
public String login(
        AuthenticationManager authenticationManager,
        String username,
        String password,
        JwtUtil jwtUtil) {

    if (!userRepository.existsByUsername(username)) {
        throw new UnauthorizedActionException(
                ErrorCode.INVALID_CREDENTIALS,
                "User does not exist with this username."
        );
    }

    try {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

    } catch (org.springframework.security.core.AuthenticationException ex) {

        throw new UnauthorizedActionException(
                ErrorCode.INVALID_CREDENTIALS,
                "Invalid password."
        );
    }

    UserIdentity identity = getUserIdentityByUsername(username);

    if (!identity.isEmailVerified()) {
        throw new UnauthorizedActionException(
                ErrorCode.EMAIL_NOT_VERIFIED,
                "Please verify your email before logging in."
        );
    }

    return jwtUtil.generateToken(identity.getUsername());
}


    // UPDATE USER (WITH CACHE EVICTION)
    @Transactional
    public void update(String userId, UserUpdateDto request) {

        UserIdentity userIdentity = authenticatedUserService.getCurrentUserIdentity();

        if (!userId.equals(userIdentity.getId())) {
            throw new InvalidRequestException(
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    "This is not your account."
            );
        }

        User user = userRepository.findById(new ObjectId(userId))
                .orElseThrow(() ->
                        new UserNotFoundException(
                                ErrorCode.USER_NOT_FOUND,
                                "User does not exist by this ID."
                        )
                );

        if(userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidRequestException(ErrorCode.USERNAME_ALREADY_EXISTS, "This username is taken. Try another.");
        }

        if(userRepository.existsByEmail(request.getEmail())) {
            throw new InvalidRequestException(ErrorCode.EMAIL_ALREADY_EXISTS, "An account already exists with this mail.");
        }

        String oldUsername = user.getUsername();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        evictUserIdentityCache(oldUsername);
    }

    // DELETE USER (WITH CACHE EVICTION)
    @Transactional
    public void delete(String userId) {

        UserIdentity userIdentity = authenticatedUserService.getCurrentUserIdentity();

        if (!userId.equals(userIdentity.getId())) {
            throw new InvalidRequestException(
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    "This is not your account."
            );
        }

        User user = userRepository.findById(new ObjectId(userId))
                .orElseThrow(() ->
                        new UserNotFoundException(
                                ErrorCode.USER_NOT_FOUND,
                                "User not found with this ID."
                        )
                );

        String oldUsername = user.getUsername();

        userRepository.delete(user);

        evictUserIdentityCache(oldUsername);
    }

    /* ==================================================
       CACHE EVICTION
       ================================================== */

    @CacheEvict(value = "userByUsername", key = "#username")
    public void evictUserIdentityCache(String username) {
        log.info("Evicted userByUsername cache for username={}", username);
    }

    // MAPPER
    private UserIdentity mapToIdentity(User user) {

        UserIdentity dto = new UserIdentity();
        dto.setId(user.getId().toHexString());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setRoles(user.getRoles());

        return dto;
    }
}

package com.abc.foodwastemanagement.service;

import com.abc.foodwastemanagement.entity.User;
import com.abc.foodwastemanagement.enums.AuthProvider;
import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.exception.OAuthException;
import com.abc.foodwastemanagement.exception.ResourceAlreadyExistsException;
import com.abc.foodwastemanagement.repository.UserRepository;
import com.abc.foodwastemanagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * PURPOSE
 * -------
 * Handles the complete Google OAuth 2.0 authentication flow.
 *
 * RESPONSIBILITIES
 * ----------------
 * 1. Exchange authorization code → Google access token
 * 2. Fetch user profile from Google
 * 3. Enforce authentication business rules
 * 4. Create or fetch local user
 * 5. Generate JWT for this system
 *
 * NOTE
 * ----
 * This class represents a SYSTEM BOUNDARY.
 * Therefore:
 * - Full exception logging is allowed here
 * - Internal errors must be converted to domain errors
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    /**
     * Google OAuth Client ID
     */
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    /**
     * Google OAuth Client Secret
     */
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * Redirect URI
     */
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    /**
     * Used to communicate with Google OAuth APIs
     */
    private final RestTemplate restTemplate;

    /**
     * User persistence layer
     */
    private final UserRepository userRepository;

    /**
     * Used to encode a random password for OAuth users
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Utility for JWT creation
     */
    private final JwtUtil jwtUtil;

    /**
     * MAIN AUTHENTICATION METHOD
     * 
     * Orchestrates the entire Google OAuth authentication flow.
     *
     * @param code Authorization code received from Google
     * @return JWT token for authenticated user
     */
    @Override
    public String authenticateWithGoogle(String code) {

        try {
            
            // STEP 1: Exchange authorization code → access token
            String accessToken = exchangeCodeForAccessToken(code);

         
            //   STEP 2: Fetch Google user profile
            Map<String, Object> userInfo = fetchGoogleUser(accessToken);

            String email = (String) userInfo.get("email");
            String name  = (String) userInfo.get("name");


            // STEP 3: Create or fetch local user
            User user = findOrCreateUser(email, name);

            // STEP 4: Generate JWT for this system
            return jwtUtil.generateToken(user.getUsername());

        }

        catch (ResourceAlreadyExistsException ex) {
            // Business rule violation -> propagate as-is
            throw ex;
        }

        catch (OAuthException ex) {
            // Already a clean domain OAuth exception
            throw ex;
        }

        catch (Exception ex) {
            // Google, network, parsing, or unexpected failures
            log.error("Google OAuth authentication failed", ex);

            throw new OAuthException(
                    ErrorCode.AUTHENTICATION_FAILED,
                    "Google authentication failed"
            );
        }
    }

    /**
     * Calls Google's token endpoint to exchange the
     * authorization code for an access token.
     */
    private String exchangeCodeForAccessToken(String code) {

        String tokenUrl = "https://oauth2.googleapis.com/token";

        // Form parameters required by Google
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        ResponseEntity<Map<String, Object>> response =
                restTemplate.exchange(
                        tokenUrl,
                        HttpMethod.POST,
                        request,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

        Map<String, Object> body = response.getBody();

        // Defensive validation of Google response
        if (body == null || !body.containsKey("access_token")) {
            throw new OAuthException(
                    ErrorCode.AUTHENTICATION_FAILED,
                    "Invalid token response from Google"
            );
        }

        return (String) body.get("access_token");
    }

    /**
     * FETCH GOOGLE USER PROFILE
     *
     * Uses the Google access token to retrieve user details.
     */
    private Map<String, Object> fetchGoogleUser(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response =
                restTemplate.exchange(
                        "https://www.googleapis.com/oauth2/v2/userinfo",
                        HttpMethod.GET,
                        request,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

        Map<String, Object> body = response.getBody();

        if (body == null || !body.containsKey("email")) {
            throw new OAuthException(
                    ErrorCode.AUTHENTICATION_FAILED,
                    "Failed to fetch Google user profile"
            );
        }

        return body;
    }

    /**
     * CREATE OR FETCH LOCAL USER
     * 
     * - If email exists AND provider != GOOGLE → reject
     * - Else create user on first login
     */
    private User findOrCreateUser(String email, String name) {

        return userRepository.findByEmail(email)
                .map(existing -> {

                    // Prevent account takeover
                    if (AuthProvider.GOOGLE != (existing.getAuthProvider())) {
                        throw new ResourceAlreadyExistsException(
                                ErrorCode.USER_ALREADY_EXISTS,
                                "Account exists with different login method"
                        );
                    }

                    return existing;

                })
                .orElseGet(() -> {

                    // First-time Google login → create user
                    User user = new User();
                    user.setEmail(email);
                    user.setUsername(name);

                    // Random password (never used)
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

                    user.setRoles(List.of("ROLE_USER"));
                    user.setAuthProvider(AuthProvider.GOOGLE);
                    user.setEmailVerified(true);

                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());

                    return userRepository.save(user);
                });
    }
}
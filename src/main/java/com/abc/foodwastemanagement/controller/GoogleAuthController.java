package com.abc.foodwastemanagement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.abc.foodwastemanagement.service.GoogleOAuthServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * =========================================================
 * GOOGLE AUTH CONTROLLER
 * =========================================================
 *
 * Responsibilities:
 * -----------------
 * - Handle HTTP request/response
 * - Delegate business logic to service layer
 *
 */
@RestController
@RequestMapping("/auth/google")
@Tag(name = "Google login API")
@Slf4j
@RequiredArgsConstructor
public class GoogleAuthController {

    /**
     * Google OAuth service abstraction
     */
    private final GoogleOAuthServiceImpl googleOAuthServiceImpl;

    /**
     * =========================================================
     * GOOGLE OAUTH CALLBACK ENDPOINT
     * =========================================================
     *
     * Google redirects user here with:
     * ?code=AUTHORIZATION_CODE
     */
    @GetMapping("/callback")
    @Operation(description = "Allows an user to login using their Google account.")
    public ResponseEntity<String> handleGoogleCallback(@RequestParam String code) {

        // Delegate authentication to service layer
        String jwt = googleOAuthServiceImpl.authenticateWithGoogle(code);

        // Return JWT to client
        return ResponseEntity.ok(jwt);
    }
}

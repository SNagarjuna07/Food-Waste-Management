package com.abc.foodwastemanagement.controller;

import com.abc.foodwastemanagement.dto.auth.ForgotPasswordRequest;
import com.abc.foodwastemanagement.dto.auth.ResetPasswordRequest;
import com.abc.foodwastemanagement.dto.user.RegisterUserRequest;
import com.abc.foodwastemanagement.dto.user.UserLoginDto;
import com.abc.foodwastemanagement.dto.user.UserUpdateDto;
import com.abc.foodwastemanagement.email.EmailVerificationService;
import com.abc.foodwastemanagement.security.JwtUtil;
import com.abc.foodwastemanagement.service.PasswordResetService;
import com.abc.foodwastemanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@Tag(name = "User APIs")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    private final PasswordResetService passwordResetService;

    private final EmailVerificationService emailVerificationService;

    private final UserService userService;
    
    @Operation(description = "Allow users to signup.")
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterUserRequest request) {

        userService.registerUser(request);

        return ResponseEntity.ok("Registration successful. The verification link has been sent to your email. Please verify your email before logging in.");
    }

    @GetMapping("/verify-email")
    @Operation(description = "Allow users to verify their email before logging in.")
    public ResponseEntity<String> verifyMail(@RequestParam String token) {

        emailVerificationService.verifyEmail(token);

        return ResponseEntity.ok("Email verified successfully. Now you can login.");
    }
    

    @PostMapping("/login")
    @Operation(description = "Allow users to login.")
    public ResponseEntity<String> login(@RequestBody UserLoginDto request) {
    
        String token = userService.login(
                authenticationManager,
                request.getUsername(),
                request.getPassword(),
                jwtUtil
        );
    
        return ResponseEntity.ok(token);
    }

    @PatchMapping("/update/{id}")
    @Operation(description = "Allow users to update.")
    public ResponseEntity<Void> updateUser(@PathVariable String id, @RequestBody UserUpdateDto request) {

        userService.update(id, request);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    } 

    @DeleteMapping("/delete/{id}")
    @Operation(description = "Allow users to delete.")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {

        userService.delete(id);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/forgot-password")
    @Operation(description = "Sends a forgot password link to the users.")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        
        passwordResetService.forgotPassword(forgotPasswordRequest.getEmail());

        return ResponseEntity.ok("Password reset link has been sent to your email. Please reset your password.");
    }

    @PostMapping("/reset-password")
    @Operation(description = "Allows user to reset their password.")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        
        passwordResetService.resetPassword(resetPasswordRequest.getToken(), resetPasswordRequest.getNewPassword());

        return ResponseEntity.ok("Password reset successful.");
    }
}

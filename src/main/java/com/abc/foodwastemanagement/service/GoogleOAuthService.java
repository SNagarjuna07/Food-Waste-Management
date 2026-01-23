package com.abc.foodwastemanagement.service;

/**
 *
 * Purpose:
 * --------
 * This interface defines the contract for Google OAuth
 * authentication in the system.
 *
 */
public interface GoogleOAuthService {

    /**
     * Authenticates a user using Google OAuth authorization code.
     *
     * Flow:
     * -----
     * 1. Accept authorization code from controller
     * 2. Exchange code → access token
     * 3. Fetch Google user profile
     * 4. Create or fetch local user
     * 5. Generate JWT
     *
     * @param authorizationCode code received from Google
     * @return JWT token for authenticated user
     */
    String authenticateWithGoogle(String authorizationCode);
}

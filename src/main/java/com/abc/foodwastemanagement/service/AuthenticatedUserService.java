package com.abc.foodwastemanagement.service;

import com.abc.foodwastemanagement.dto.user.UserIdentity;
import com.abc.foodwastemanagement.entity.User;
import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.exception.UnauthorizedActionException;
import com.abc.foodwastemanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserIdentity getCurrentUserIdentity() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new UnauthorizedActionException(
                    ErrorCode.UNAUTHORIZED_ACTION,
                    "User is not authenticated"
            );
        }

        String username;

        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = authentication.getName();
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UnauthorizedActionException(
                    ErrorCode.UNAUTHORIZED_ACTION,
                    "Authenticated user not found"
            );
        }

        return mapToIdentity(user);
    }

    @Transactional(readOnly = true)
    public ObjectId getCurrentUserId() {
        return new ObjectId(getCurrentUserIdentity().getId());
    }

    /* ===================== MAPPER ===================== */

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

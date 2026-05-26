package com.abc.foodwastemanagement.controller;

import com.abc.foodwastemanagement.dto.notification.NotificationResponse;
import com.abc.foodwastemanagement.dto.page.PageResponse;
import com.abc.foodwastemanagement.dto.user.UserIdentity;
import com.abc.foodwastemanagement.notification.NotificationServiceImpl;
import com.abc.foodwastemanagement.service.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@Tag(name = "Notification APIs")
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final AuthenticatedUserService authenticatedUserService;
    
    private final NotificationServiceImpl notificationServiceImpl;

    /* ---------------- GET ALL NOTIFICATIONS (PAGINATED) ---------------- */
    @GetMapping
    @Operation(description = "Get all the notifications of an user.")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        return ResponseEntity.ok(
                notificationServiceImpl.getUserNotifications(new ObjectId(user.getId()), page, size)
        );
    }

    /* ---------------- GET UNREAD COUNT ---------------- */
    @GetMapping("/unread-count")
    @Operation(description = "Get the unread notifications count of an user.")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        return ResponseEntity.ok(
                Map.of("unread", notificationServiceImpl.getUnreadCount(new ObjectId(user.getId())))
        );
    }

    /* ---------------- MARK SINGLE NOTIFICATION AS READ ---------------- */
    @PatchMapping("/{id}/read")
    @Operation(description = "Mark a notification as read of an user.")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        notificationServiceImpl.markAsRead(id, new ObjectId(user.getId()));

        return ResponseEntity.noContent().build();
    }

    /* ---------------- MARK ALL AS READ ---------------- */
    @PatchMapping("/read-all")
    @Operation(description = "Make all the notifications read of an user.")
    public ResponseEntity<Void> markAllAsRead() {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        notificationServiceImpl.markAllAsRead(new ObjectId(user.getId()));

        return ResponseEntity.noContent().build();
    }
}
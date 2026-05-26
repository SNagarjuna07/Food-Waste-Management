package com.abc.foodwastemanagement.notification;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abc.foodwastemanagement.dto.notification.NotificationResponse;
import com.abc.foodwastemanagement.dto.page.PageResponse;
import com.abc.foodwastemanagement.entity.Notification;
import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.enums.KafkaNotificationType;
import com.abc.foodwastemanagement.enums.NotificationType;
import com.abc.foodwastemanagement.exception.ResourceNotFoundException;
import com.abc.foodwastemanagement.exception.UnauthorizedActionException;
import com.abc.foodwastemanagement.notification.event.NotificationEvent;
import com.abc.foodwastemanagement.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    /* ================= CREATE ================= */

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userNotifications", key = "#userId.toHexString() + ':' + 0 + ':' + 10", condition = "#userId != null"),
        @CacheEvict(value = "userUnreadNotificationCount", key = "#userId.toHexString()", condition = "#userId != null")
    })
    @Override
    public void saveNotification(
            ObjectId userId,
            String title,
            String message,
            NotificationType type) {

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    @Transactional
    @Override
    public void createFromEvent(NotificationEvent event) {

        ObjectId userId = new ObjectId(event.getUserId());

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(event.getTitle());
        notification.setMessage(event.getMessage());
        notification.setType(mapType(event.getEventType()));
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    /* ================= READ (CACHED) ================= */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = "userNotifications",
        key = "#userId.toHexString() + ':' + #page + ':' + #size",
        unless = "#result == null || #result.content.isEmpty()"
    )
    public PageResponse<NotificationResponse> getUserNotifications(
            ObjectId userId,
            int page,
            int size) {

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<NotificationResponse> result =
                notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        .map(this::mapToResponse);

        return PageResponse.from(result);
    }

    /* ================= COUNT (CACHED) ================= */
    @Transactional(readOnly = true)
    @Cacheable(
        value = "userUnreadNotificationCount",
        key = "#userId.toHexString()"
    )
    public long getUnreadCount(ObjectId userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /* ================= UPDATE ================= */

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userNotifications", allEntries = true),
        @CacheEvict(value = "userUnreadNotificationCount", key = "#userId.toHexString()")
    })
    public void markAsRead(String notificationId, ObjectId userId) {

        ObjectId id = new ObjectId(notificationId);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.NOTIFICATION_NOT_FOUND,
                        "Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedActionException(
                    ErrorCode.UNAUTHORIZED_ACTION,
                    "Not your notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userNotifications", allEntries = true),
        @CacheEvict(value = "userUnreadNotificationCount", key = "#userId.toHexString()")
    })
    public void markAllAsRead(ObjectId userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /* ================= MAPPER ================= */

    private NotificationResponse mapToResponse(Notification n) {

        return new NotificationResponse(
                n.getId().toHexString(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    private NotificationType mapType(KafkaNotificationType kafkaType) {
        return switch (kafkaType) {
            case EMAIL_VERIFIED -> NotificationType.INFO;
            case PASSWORD_RESET_REQUESTED -> NotificationType.ACTION_REQUIRED;
            case PASSWORD_RESET_COMPLETED -> NotificationType.INFO;
            case DONATION_COLLECTED -> NotificationType.INFO;
        };
    }
}

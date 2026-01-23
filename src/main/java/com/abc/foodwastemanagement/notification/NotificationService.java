package com.abc.foodwastemanagement.notification;

import org.bson.types.ObjectId;

import com.abc.foodwastemanagement.dto.notification.NotificationResponse;
import com.abc.foodwastemanagement.dto.page.PageResponse;
import com.abc.foodwastemanagement.enums.NotificationType;
import com.abc.foodwastemanagement.notification.event.NotificationEvent;

public interface NotificationService {

    void saveNotification(ObjectId userId, String title, String message, NotificationType type);

    void createFromEvent(NotificationEvent event);

    PageResponse<NotificationResponse> getUserNotifications(ObjectId userId, int page, int size);
}

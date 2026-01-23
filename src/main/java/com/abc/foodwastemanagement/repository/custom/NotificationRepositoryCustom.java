package com.abc.foodwastemanagement.repository.custom;

import org.bson.types.ObjectId;

public interface NotificationRepositoryCustom {

    void markAllAsRead(ObjectId userId);
    
}

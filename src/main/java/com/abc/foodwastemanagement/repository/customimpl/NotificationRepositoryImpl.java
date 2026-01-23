package com.abc.foodwastemanagement.repository.customimpl;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.abc.foodwastemanagement.entity.Notification;
import com.abc.foodwastemanagement.repository.custom.NotificationRepositoryCustom;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void markAllAsRead(ObjectId userId) {

        Query query = new Query(
            Criteria.where("userId").is(userId)
                    .and("read").is(false)
        );

        Update update = new Update().set("read", true);

        mongoTemplate.updateMulti(query, update, Notification.class);
    }
}

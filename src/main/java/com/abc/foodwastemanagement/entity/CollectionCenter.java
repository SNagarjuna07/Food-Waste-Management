package com.abc.foodwastemanagement.entity;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "collection_centers")
@CompoundIndex(
    name = "active_created_idx",
    def = "{ 'active': 1, 'createdAt': -1 }"
)
public class CollectionCenter {

    @Id
    private ObjectId id;

    @Indexed
    private String name;
    private String location;
    private Integer capacity;

    private boolean active;

    private LocalDateTime createdAt;
}

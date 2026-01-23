package com.abc.foodwastemanagement.dto.collectioncenter;

import lombok.Data;

@Data
public class CollectionCenterRequest {

    private String name;
    private String location;
    private Integer capacity;

    private boolean active;
}

package com.abc.foodwastemanagement.dto.collectioncenter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionCenterResponse {

    private String id;
    private String name;
    private Integer capacity;
    private String location;
    private boolean active;
}

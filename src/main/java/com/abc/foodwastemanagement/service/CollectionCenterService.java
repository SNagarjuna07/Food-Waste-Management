package com.abc.foodwastemanagement.service;

import com.abc.foodwastemanagement.dto.collectioncenter.CollectionCenterRequest;
import com.abc.foodwastemanagement.dto.collectioncenter.CollectionCenterResponse;
import com.abc.foodwastemanagement.dto.page.PageResponse;
import com.abc.foodwastemanagement.entity.CollectionCenter;
import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.exception.ResourceNotFoundException;
import com.abc.foodwastemanagement.repository.CollectionCenterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionCenterService {

    private final CollectionCenterRepository collectionCenterRepository;

    // ========================= ADMIN =========================

    @Transactional(readOnly = true)
    @Cacheable(
        value = "activeCollectionCenters",
        key = "'page=' + #page + '&size=' + #size",
        unless = "#result == null || #result.content.isEmpty()"
    )
    // Returns all active centers 
    public PageResponse<CollectionCenterResponse> getActiveCenters(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<CollectionCenterResponse> result =
                collectionCenterRepository
                        .findByActiveTrueOrderByCreatedAtDesc(pageable)
                        .map(this::mapToResponse);

        return PageResponse.from(result);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "collectionCenters", allEntries = true),
        @CacheEvict(value = "activeCollectionCenters", allEntries = true)
    })

    // Center can be created only by ADMIN
    public CollectionCenter createCenter(CollectionCenterRequest request) {

        CollectionCenter center = new CollectionCenter();
        center.setName(request.getName());
        center.setLocation(request.getLocation());
        center.setCapacity(request.getCapacity());
        center.setActive(request.isActive());

        return collectionCenterRepository.save(center);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "collectionCenters", allEntries = true),
        @CacheEvict(value = "activeCollectionCenters", allEntries = true),
        @CacheEvict(value = "collectionCenterById", key = "#id.toHexString()")
    })

    // Update center can be done only by ADMIN
    public CollectionCenter updateCenter(ObjectId id, CollectionCenterRequest request) {

        CollectionCenter center = getById(id);

        center.setName(request.getName());
        center.setLocation(request.getLocation());
        center.setCapacity(request.getCapacity());
        center.setActive(request.isActive());

        return collectionCenterRepository.save(center);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "collectionCenters", allEntries = true),
        @CacheEvict(value = "activeCollectionCenters", allEntries = true),
        @CacheEvict(value = "collectionCenterById", key = "#id.toHexString()")
    })

    // Delete a center (ADMIN)
    public void deleteCenter(ObjectId id) {

        CollectionCenter center = getById(id);
        collectionCenterRepository.delete(center);
    }

    // ========================= USER =========================


    @Transactional(readOnly = true)

    // Returns a center by its ID
    protected CollectionCenter getById(ObjectId id) {
        return collectionCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COLLECTION_CENTER_NOT_FOUND,
                        "Collection center not found"));
    }

    
    @Transactional(readOnly = true)
    @Cacheable(
        value = "collectionCenterById",
        key = "#id.toHexString()",
        unless = "#result == null"
    )
    public CollectionCenterResponse getResponseById(ObjectId id) {

        log.info("Hitting DB for center id={}", id.toHexString());

        CollectionCenter center = getById(id);
        return mapToResponse(center);
    }

    @Transactional(readOnly = true)
    @Cacheable(
        value = "collectionCenters",
        key = "'page=' + #page + '&size=' + #size",
        unless = "#result == null || #result.content.isEmpty()"
    )

    // Returns all the centers
    public PageResponse<CollectionCenterResponse> getAll(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<CollectionCenterResponse> result =
                collectionCenterRepository.findAll(pageable)
                        .map(this::mapToResponse);

        return PageResponse.from(result);
    }

    // ========================= MAPPER =========================

    private CollectionCenterResponse mapToResponse(CollectionCenter center) {
        return new CollectionCenterResponse(
                center.getId().toHexString(),
                center.getName(),
                center.getCapacity(),
                center.getLocation(),
                center.isActive()
        );
    }
}

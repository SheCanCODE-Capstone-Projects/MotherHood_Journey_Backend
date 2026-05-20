package com.motherhood.journey.government.repository;

import com.motherhood.journey.government.entity.ServiceRequestDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceRequestDocRepository extends JpaRepository<ServiceRequestDoc, UUID> {
    List<ServiceRequestDoc> findByRequest_Id(UUID requestId);
}

package com.motherhood.journey.child.repository;

import com.motherhood.journey.child.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ChildRepository extends JpaRepository<Child, UUID> {
}

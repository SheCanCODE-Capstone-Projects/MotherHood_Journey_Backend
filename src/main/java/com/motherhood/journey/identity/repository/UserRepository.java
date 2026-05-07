
package com.motherhood.journey.identity.repository;

import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByNationalId(String nationalId);


    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByNationalId(String nationalId);

    List<User> findByFacility_IdAndRole(UUID facilityId, UserRole role);
    List<User> findByFacility_Id(UUID facilityId);

    List<User> findByRole(UserRole role);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :time WHERE u.id = :id")
    void updateLastLogin(UUID id, LocalDateTime time);

    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.id = :id")
    void deactivateUser(UUID id);
}
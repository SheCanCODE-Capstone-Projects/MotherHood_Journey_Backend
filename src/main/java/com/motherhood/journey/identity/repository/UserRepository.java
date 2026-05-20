package com.motherhood.journey.identity.repository;

import com.motherhood.journey.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByNationalId(String nationalId);

    @Query("SELECT u FROM User u WHERE u.role = 'DISTRICT_OFFICER' " +
           "AND u.facility.district = :district AND u.active = true ORDER BY u.createdAt ASC")
    Optional<User> findDistrictOfficerByDistrict(@Param("district") String district);
}

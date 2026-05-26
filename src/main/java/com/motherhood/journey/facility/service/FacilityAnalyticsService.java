package com.motherhood.journey.facility.service;

import com.motherhood.journey.facility.dto.response.FacilityAnalyticsResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FacilityAnalyticsService {

    private final JdbcTemplate jdbc;

    public FacilityAnalyticsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public FacilityAnalyticsResponse computeFor(UUID facilityId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate thirtyDaysAgo = now.toLocalDate().minusDays(30);

        long activeMothers = scalar(
            "SELECT COUNT(*) FROM mothers m JOIN users u ON m.user_id = u.id " +
            "WHERE m.facility_id = ? AND u.active = TRUE", facilityId);

        long ancVisits30d = scalar(
            "SELECT COUNT(*) FROM health_visits hv " +
            "WHERE hv.facility_id = ? AND hv.visit_type = 'ANC' AND hv.visit_date >= ?",
            facilityId, thirtyDaysAgo);

        long expectedAncVisits = scalar(
            "SELECT COUNT(*) FROM pregnancies p JOIN mothers m ON p.mother_id = m.id " +
            "WHERE m.facility_id = ? AND p.status = 'ACTIVE'", facilityId);
        double ancAttendanceRate = rate(ancVisits30d, expectedAncVisits);

        long vaxDue = scalar(
            "SELECT COUNT(*) FROM vaccination_records vr " +
            "WHERE vr.facility_id = ? AND vr.status IN ('PENDING','OVERDUE')", facilityId);
        long vaxAdministered = scalar(
            "SELECT COUNT(*) FROM vaccination_records vr " +
            "WHERE vr.facility_id = ? AND vr.status = 'ADMINISTERED'", facilityId);
        double vaxCoverage = rate(vaxAdministered, vaxDue + vaxAdministered);

        long apptScheduled = scalar(
            "SELECT COUNT(*) FROM appointments a " +
            "WHERE a.facility_id = ? AND a.scheduled_at >= ?", facilityId, now.minusDays(30));
        long apptNoShow = scalar(
            "SELECT COUNT(*) FROM appointments a " +
            "WHERE a.facility_id = ? AND a.status = 'NO_SHOW' AND a.scheduled_at >= ?",
            facilityId, now.minusDays(30));
        double noShowRate = rate(apptNoShow, apptScheduled);

        long srPending = scalar(
            "SELECT COUNT(*) FROM service_requests sr " +
            "WHERE sr.facility_id = ? AND sr.status = 'PENDING'", facilityId);
        long srOverdue = scalar(
            "SELECT COUNT(*) FROM service_requests sr " +
            "WHERE sr.facility_id = ? AND sr.status = 'PENDING' AND sr.created_at < ?",
            facilityId, now.minusHours(48));

        return new FacilityAnalyticsResponse(
            facilityId,
            activeMothers,
            ancVisits30d,
            ancAttendanceRate,
            vaxDue,
            vaxAdministered,
            vaxCoverage,
            apptScheduled,
            apptNoShow,
            noShowRate,
            srPending,
            srOverdue);
    }

    private long scalar(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : Math.round((numerator * 10000.0) / denominator) / 100.0;
    }
}

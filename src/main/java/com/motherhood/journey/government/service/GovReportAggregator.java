package com.motherhood.journey.government.service;

import com.motherhood.journey.government.enums.ReportType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GovReportAggregator {

    private final JdbcTemplate jdbc;

    public GovReportAggregator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> compute(ReportType type, String period, UUID geoLocationId) {
        Period p = parsePeriod(period);
        return switch (type) {
            case VACCINATION_COVERAGE -> vaccinationCoverage(p, geoLocationId);
            case ANC_ATTENDANCE       -> ancAttendance(p, geoLocationId);
            case BIRTH_REGISTRATION   -> birthRegistration(p, geoLocationId);
            case MATERNAL_HEALTH      -> maternalHealth(p, geoLocationId);
        };
    }

    private Map<String, Object> vaccinationCoverage(Period p, UUID geoId) {
        long administered = scalar("""
            SELECT COUNT(*) FROM vaccination_records vr
            JOIN children c ON vr.child_id = c.id
            WHERE c.geo_location_id = ? AND vr.status = 'ADMINISTERED'
              AND vr.administered_date BETWEEN ? AND ?
            """, geoId, p.start, p.end);
        long due = scalar("""
            SELECT COUNT(*) FROM vaccination_records vr
            JOIN children c ON vr.child_id = c.id
            WHERE c.geo_location_id = ? AND vr.due_date BETWEEN ? AND ?
            """, geoId, p.start, p.end);
        return ordered(
            "administered", administered,
            "due", due,
            "coverage_pct", rate(administered, due));
    }

    private Map<String, Object> ancAttendance(Period p, UUID geoId) {
        long visits = scalar("""
            SELECT COUNT(*) FROM health_visits hv
            JOIN mothers m ON hv.patient_ref_id = m.id
            WHERE hv.patient_type = 'MOTHER' AND m.geo_location_id = ? AND hv.visit_type = 'ANC'
              AND hv.visit_datetime BETWEEN ? AND ?
            """, geoId, p.start.atStartOfDay(), p.end.atStartOfDay());
        long activePregnancies = scalar("""
            SELECT COUNT(*) FROM pregnancies pr
            JOIN mothers m ON pr.mother_id = m.id
            WHERE m.geo_location_id = ? AND pr.status = 'ACTIVE'
            """, geoId);
        return ordered(
            "anc_visits", visits,
            "active_pregnancies", activePregnancies,
            "attendance_pct", rate(visits, activePregnancies));
    }

    private Map<String, Object> birthRegistration(Period p, UUID geoId) {
        long births = scalar("""
            SELECT COUNT(*) FROM children c
            WHERE c.geo_location_id = ? AND c.date_of_birth BETWEEN ? AND ?
            """, geoId, p.start, p.end);
        long registered = scalar("""
            SELECT COUNT(*) FROM children c
            WHERE c.geo_location_id = ? AND c.date_of_birth BETWEEN ? AND ?
              AND c.birth_certificate_no IS NOT NULL
            """, geoId, p.start, p.end);
        return ordered(
            "births", births,
            "registered", registered,
            "registration_pct", rate(registered, births));
    }

    private Map<String, Object> maternalHealth(Period p, UUID geoId) {
        long delivered = scalar("""
            SELECT COUNT(*) FROM pregnancies pr
            JOIN mothers m ON pr.mother_id = m.id
            WHERE m.geo_location_id = ? AND pr.status = 'DELIVERED'
              AND pr.updated_at BETWEEN ? AND ?
            """, geoId, p.start.atStartOfDay(), p.end.atStartOfDay());
        long lost = scalar("""
            SELECT COUNT(*) FROM pregnancies pr
            JOIN mothers m ON pr.mother_id = m.id
            WHERE m.geo_location_id = ? AND pr.status = 'LOST'
              AND pr.updated_at BETWEEN ? AND ?
            """, geoId, p.start.atStartOfDay(), p.end.atStartOfDay());
        long active = scalar("""
            SELECT COUNT(*) FROM pregnancies pr
            JOIN mothers m ON pr.mother_id = m.id
            WHERE m.geo_location_id = ? AND pr.status = 'ACTIVE'
            """, geoId);
        return ordered(
            "delivered", delivered,
            "lost", lost,
            "active", active);
    }

    private long scalar(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
    }

    private double rate(long n, long d) {
        return d == 0 ? 0.0 : Math.round((n * 10000.0) / d) / 100.0;
    }

    private static Map<String, Object> ordered(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private Period parsePeriod(String period) {
        if (period == null) throw new IllegalArgumentException("period required");
        try {
            YearMonth ym = YearMonth.parse(period);
            return new Period(ym.atDay(1), ym.atEndOfMonth());
        } catch (DateTimeParseException ignored) {}
        LocalDate today = LocalDate.now();
        return new Period(today.minusMonths(1), today);
    }

    private record Period(LocalDate start, LocalDate end) {}
}

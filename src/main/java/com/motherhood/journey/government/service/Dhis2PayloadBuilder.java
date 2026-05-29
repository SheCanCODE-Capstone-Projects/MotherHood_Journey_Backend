package com.motherhood.journey.government.service;

import com.motherhood.journey.government.enums.ReportType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates a (ReportType, period, geoLocationId, aggregates) tuple into a
 * DHIS2-compatible dataValueSet payload.
 *
 * Spec reference: DHIS2 Web API "dataValueSets" endpoint.
 *  - dataSet:    String  (configured per ReportType)
 *  - period:     ISO     (YYYYMM for monthly, e.g. 202605)
 *  - orgUnit:    String  (geoLocationId mapped to DHIS2 orgUnit)
 *  - dataValues: Array of {dataElement, value, categoryOptionCombo?}
 */
public final class Dhis2PayloadBuilder {

    private Dhis2PayloadBuilder() {}

    public static Map<String, Object> build(ReportType reportType,
                                             String period,
                                             String orgUnit,
                                             Map<String, Object> aggregates) {
        String dataSet = dataSetFor(reportType);
        String dhis2Period = toDhis2Period(period);

        List<Map<String, Object>> dataValues = new ArrayList<>();
        aggregates.forEach((key, value) -> {
            String dataElement = dataElementFor(reportType, key);
            if (dataElement == null) {
                return;
            }
            Map<String, Object> dv = new LinkedHashMap<>();
            dv.put("dataElement", dataElement);
            dv.put("value", value);
            dataValues.add(dv);
        });

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dataSet", dataSet);
        payload.put("period", dhis2Period);
        payload.put("orgUnit", orgUnit);
        payload.put("completeDate", LocalDate.now().toString());
        payload.put("dataValues", dataValues);
        return payload;
    }

    private static String dataSetFor(ReportType type) {
        return switch (type) {
            case VACCINATION_COVERAGE -> "DS_EPI_MONTHLY";
            case ANC_ATTENDANCE       -> "DS_ANC_MONTHLY";
            case BIRTH_REGISTRATION   -> "DS_BIRTH_REG_MONTHLY";
            case MATERNAL_HEALTH      -> "DS_MAT_HEALTH_MONTHLY";
        };
    }

    /**
     * Maps internal aggregate keys to DHIS2 dataElement UIDs.
     * Real UIDs should come from the MoH DHIS2 instance — this is a stable
     * mapping table that ops can update without code changes if needed.
     */
    private static String dataElementFor(ReportType type, String key) {
        return switch (type) {
            case VACCINATION_COVERAGE -> switch (key) {
                case "administered" -> "DE_VAX_ADMINISTERED";
                case "due"          -> "DE_VAX_DUE";
                case "coverage_pct" -> "DE_VAX_COVERAGE_PCT";
                default -> null;
            };
            case ANC_ATTENDANCE -> switch (key) {
                case "anc_visits"         -> "DE_ANC_VISITS";
                case "active_pregnancies" -> "DE_ANC_ACTIVE_PREG";
                case "attendance_pct"     -> "DE_ANC_ATTENDANCE_PCT";
                default -> null;
            };
            case BIRTH_REGISTRATION -> switch (key) {
                case "births"           -> "DE_BIRTHS_TOTAL";
                case "registered"       -> "DE_BIRTHS_REGISTERED";
                case "registration_pct" -> "DE_BIRTH_REG_PCT";
                default -> null;
            };
            case MATERNAL_HEALTH -> switch (key) {
                case "delivered" -> "DE_MAT_DELIVERED";
                case "lost"      -> "DE_MAT_LOST";
                case "active"    -> "DE_MAT_ACTIVE";
                default -> null;
            };
        };
    }

    private static String toDhis2Period(String period) {
        if (period == null) {
            return "";
        }
        return period.replace("-", "");
    }
}

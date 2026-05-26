package com.motherhood.journey.maternal.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

/**
 * Validates ICD-10 codes against a seeded subset from the Rwanda MoH HMIS.
 * Extend MOH_HMIS_CODES as more codes are confirmed with the MoH team.
 */
public class Icd10CodeValidator implements ConstraintValidator<Icd10Code, String> {

    /**
     * Seeded MoH HMIS ICD-10 subset — maternal and child health focused.
     * Source: Rwanda MoH HMIS code list (Sprint 1 subset).
     */
    private static final Set<String> MOH_HMIS_CODES = Set.of(
            // Pregnancy & childbirth
            "O00", "O10", "O11", "O12", "O13", "O14", "O15", "O16",
            "O20", "O21", "O22", "O23", "O24", "O25", "O26",
            "O30", "O31", "O32", "O33", "O34", "O35", "O36",
            "O40", "O41", "O42", "O43", "O44", "O45", "O46", "O47", "O48",
            "O60", "O61", "O62", "O63", "O64", "O65", "O66", "O67", "O68", "O69",
            "O70", "O71", "O72", "O73", "O74", "O75",
            "O80", "O82", "O85", "O86", "O87", "O88", "O89", "O90",
            // Newborn / perinatal
            "P00", "P01", "P02", "P03", "P04", "P05", "P07", "P08",
            "P10", "P11", "P12", "P13", "P14", "P15",
            "P20", "P21", "P22", "P23", "P24", "P25", "P26", "P27", "P28", "P29",
            "P35", "P36", "P37", "P38", "P39",
            "P50", "P51", "P52", "P53", "P54", "P55", "P56", "P57", "P58", "P59",
            "P70", "P71", "P72", "P74", "P76", "P77", "P78",
            "P80", "P81", "P83", "P84", "P90", "P91", "P92", "P93", "P94", "P96",
            // Malaria
            "B50", "B51", "B52", "B53", "B54",
            // Anaemia
            "D50", "D51", "D52", "D53",
            // Malnutrition
            "E40", "E41", "E42", "E43", "E44", "E45", "E46",
            // Respiratory
            "J00", "J01", "J02", "J03", "J04", "J05", "J06",
            "J10", "J11", "J12", "J13", "J14", "J15", "J16", "J17", "J18",
            "J20", "J21", "J22",
            // Diarrhoea
            "A00", "A01", "A02", "A03", "A04", "A05", "A06", "A07", "A08", "A09",
            // HIV
            "B20", "B21", "B22", "B23", "B24",
            // Hypertension
            "I10", "I11", "I12", "I13", "I15",
            // Diabetes
            "E10", "E11", "E12", "E13", "E14"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        // Normalise: trim and uppercase, strip trailing decimal (e.g. O14.1 → O14)
        String normalised = value.trim().toUpperCase().replaceAll("\\..*", "");
        return MOH_HMIS_CODES.contains(normalised);
    }
}

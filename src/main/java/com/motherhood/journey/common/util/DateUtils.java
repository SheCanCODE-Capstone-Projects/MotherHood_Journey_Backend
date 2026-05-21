package com.motherhood.journey.common.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", java.util.Locale.ROOT);

    private DateUtils() {}

    /** Returns age in completed years from dateOfBirth to today. */
    public static int ageInYears(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /** Returns age in completed months from dateOfBirth to today. */
    public static int ageInMonths(LocalDate dateOfBirth) {
        Period p = Period.between(dateOfBirth, LocalDate.now());
        return p.getYears() * 12 + p.getMonths();
    }

    /** Returns age in completed days from dateOfBirth to today. */
    public static long ageInDays(LocalDate dateOfBirth) {
        return dateOfBirth.until(LocalDate.now(), java.time.temporal.ChronoUnit.DAYS);
    }

    /** Formats a LocalDate as dd/MM/yyyy for display. */
    public static String format(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_FORMAT);
    }

    /** Returns true if the date is in the past (before today). */
    public static boolean isPast(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }

    /** Calculates Estimated Due Date from Last Menstrual Period using Naegele's rule. */
    public static LocalDate calculateEdd(LocalDate lmpDate) {
        return lmpDate.plusDays(280);
    }

    /** Returns gestational age in weeks from LMP to today. */
    public static int gestationalWeeks(LocalDate lmpDate) {
        return (int) (lmpDate.until(LocalDate.now(), java.time.temporal.ChronoUnit.DAYS) / 7);
    }
}

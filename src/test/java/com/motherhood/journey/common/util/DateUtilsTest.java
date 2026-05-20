package com.motherhood.journey.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    @Test
    void ageInYears_pastDate_returnsCorrectAge() {
        LocalDate dob = LocalDate.now().minusYears(25);
        assertThat(DateUtils.ageInYears(dob)).isEqualTo(25);
    }

    @Test
    void ageInMonths_threeMonthsAgo_returnsAtLeast3() {
        LocalDate dob = LocalDate.now().minusMonths(3);
        assertThat(DateUtils.ageInMonths(dob)).isGreaterThanOrEqualTo(3);
    }

    @Test
    void calculateEdd_fromLmp_returns280DaysLater() {
        LocalDate lmp = LocalDate.of(2026, 1, 1);
        assertThat(DateUtils.calculateEdd(lmp)).isEqualTo(LocalDate.of(2026, 10, 8));
    }

    @Test
    void isPast_yesterday_returnsTrue() {
        assertThat(DateUtils.isPast(LocalDate.now().minusDays(1))).isTrue();
    }

    @Test
    void isPast_tomorrow_returnsFalse() {
        assertThat(DateUtils.isPast(LocalDate.now().plusDays(1))).isFalse();
    }

    @Test
    void format_validDate_returnsDisplayFormat() {
        assertThat(DateUtils.format(LocalDate.of(2026, 5, 16))).isEqualTo("16/05/2026");
    }

    @Test
    void format_null_returnsEmptyString() {
        assertThat(DateUtils.format(null)).isEmpty();
    }

    @Test
    void gestationalWeeks_40WeeksAgo_returns40() {
        LocalDate lmp = LocalDate.now().minusWeeks(40);
        assertThat(DateUtils.gestationalWeeks(lmp)).isEqualTo(40);
    }
}

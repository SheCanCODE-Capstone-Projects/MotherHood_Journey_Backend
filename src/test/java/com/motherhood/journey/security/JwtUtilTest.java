package com.motherhood.journey.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "test-secret-key-minimum-32-chars-long!!";
    private static final UUID FACILITY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
        jwtUtil.validateSecret();
    }

    @Test
    void generateAndExtractPhoneNumber() {
        String token = jwtUtil.generateToken("+250788000001", "HEALTH_WORKER", FACILITY_ID);
        assertThat(jwtUtil.extractPhoneNumber(token)).isEqualTo("+250788000001");
    }

    @Test
    void generateAndExtractFacilityId() {
        String token = jwtUtil.generateToken("+250788000001", "HEALTH_WORKER", FACILITY_ID);
        assertThat(jwtUtil.extractFacilityId(token)).isEqualTo(FACILITY_ID);
    }

    @Test
    void generateAndExtractRole() {
        String token = jwtUtil.generateToken("+250788000001", "FACILITY_ADMIN", FACILITY_ID);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("FACILITY_ADMIN");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("+250788000001", "HEALTH_WORKER", FACILITY_ID);
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("not.a.valid.token")).isFalse();
    }

    @Test
    void extractFacilityId_nullFacility_returnsNull() {
        String token = jwtUtil.generateToken("+250788000001", "MOH_ADMIN", null);
        assertThat(jwtUtil.extractFacilityId(token)).isNull();
    }

    @Test
    void validateSecret_shortSecret_throwsIllegalState() {
        JwtUtil weak = new JwtUtil();
        ReflectionTestUtils.setField(weak, "secret", "short");
        ReflectionTestUtils.setField(weak, "expirationMs", 86400000L);

        assertThatThrownBy(weak::validateSecret)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32 characters");
    }
}

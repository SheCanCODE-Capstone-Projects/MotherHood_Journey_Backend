-- ============================================================
-- V27__Seed_Named_Test_Accounts.sql
-- Named test accounts for every role + 5 realistic facilities.
--
-- ALL passwords: Test@1234
-- BCrypt-10 hash: $2b$10$Z8DKO3zJOHz/HNS8ndNADOxpCnyJT0M4EzE3yghGZiXzTY7hnahpu
--
-- Quick-reference login table
-- ┌──────────────────┬────────────────────┬─────────────────────┬────────────────┐
-- │ Role             │ Phone              │ National ID         │ Name           │
-- ├──────────────────┼────────────────────┼─────────────────────┼────────────────┤
-- │ MOH_ADMIN        │ +250788900001      │ 1198001000000001    │ Jean-Baptiste H│
-- │ FACILITY_ADMIN   │ +250788900002      │ 1198501000000002    │ Immaculée U    │
-- │ FACILITY_ADMIN   │ +250788900003      │ 1199001000000003    │ Patrick N      │
-- │ FACILITY_ADMIN   │ +250788900004      │ 1198701000000004    │ Chantal M      │
-- │ DISTRICT_OFFICER │ +250788900005      │ 1197501000000005    │ Alain B        │
-- │ DISTRICT_OFFICER │ +250788900006      │ 1197801000000006    │ Sylvie I       │
-- │ HEALTH_WORKER    │ +250788900007      │ 1199501000000007    │ Eugenie M      │
-- │ HEALTH_WORKER    │ +250788900008      │ 1199201000000008    │ Clément H      │
-- │ HEALTH_WORKER    │ +250788900009      │ 1199801000000009    │ Vestine N      │
-- │ HEALTH_WORKER    │ +250788900010      │ 1199001000000010    │ Janvier T      │
-- │ MOTHER           │ +250788900011      │ 1199601000000011    │ Goretti U      │
-- │ MOTHER           │ +250788900012      │ 1199501000000012    │ Yvette U       │
-- │ MOTHER           │ +250788900013      │ 1199701000000013    │ Pascaline I    │
-- └──────────────────┴────────────────────┴─────────────────────┴────────────────┘
-- ============================================================

DO $$
DECLARE
    v_pw_hash  TEXT := '$2b$10$Z8DKO3zJOHz/HNS8ndNADOxpCnyJT0M4EzE3yghGZiXzTY7hnahpu';

    -- geo IDs resolved at runtime from V24 data
    v_geo_nyarugenge  UUID;
    v_geo_gasabo      UUID;
    v_geo_kicukiro    UUID;
    v_geo_rwamagana   UUID;
    v_geo_huye        UUID;

    -- facility IDs
    v_fac_nyarugenge  UUID;
    v_fac_gasabo      UUID;
    v_fac_kicukiro    UUID;
    v_fac_rwamagana   UUID;
    v_fac_huye        UUID;

    -- user IDs
    v_uid_moh         UUID;
    v_uid_fadm1       UUID;
    v_uid_fadm2       UUID;
    v_uid_fadm3       UUID;
    v_uid_dist1       UUID;
    v_uid_dist2       UUID;
    v_uid_hw1         UUID;
    v_uid_hw2         UUID;
    v_uid_hw3         UUID;
    v_uid_hw4         UUID;
    v_uid_mom1        UUID;
    v_uid_mom2        UUID;
    v_uid_mom3        UUID;

    -- mother IDs
    v_mid1            UUID;
    v_mid2            UUID;
    v_mid3            UUID;

BEGIN

    -- ── 1. Resolve geo locations ──────────────────────────────────────
    SELECT id INTO v_geo_nyarugenge FROM geo_locations
    WHERE province = 'Kigali City' AND district = 'Nyarugenge' LIMIT 1;

    SELECT id INTO v_geo_gasabo FROM geo_locations
    WHERE province = 'Kigali City' AND district = 'Gasabo' LIMIT 1;

    SELECT id INTO v_geo_kicukiro FROM geo_locations
    WHERE province = 'Kigali City' AND district = 'Kicukiro' LIMIT 1;

    SELECT id INTO v_geo_rwamagana FROM geo_locations
    WHERE province = 'Eastern Province' AND district = 'Rwamagana' LIMIT 1;

    SELECT id INTO v_geo_huye FROM geo_locations
    WHERE province = 'Southern Province' AND district = 'Huye' LIMIT 1;

    -- fall back to any active geo if a district is missing
    IF v_geo_nyarugenge IS NULL THEN SELECT id INTO v_geo_nyarugenge FROM geo_locations WHERE active = TRUE LIMIT 1; END IF;
    IF v_geo_gasabo     IS NULL THEN v_geo_gasabo     := v_geo_nyarugenge; END IF;
    IF v_geo_kicukiro   IS NULL THEN v_geo_kicukiro   := v_geo_nyarugenge; END IF;
    IF v_geo_rwamagana  IS NULL THEN v_geo_rwamagana  := v_geo_nyarugenge; END IF;
    IF v_geo_huye       IS NULL THEN v_geo_huye       := v_geo_nyarugenge; END IF;

    IF v_geo_nyarugenge IS NULL THEN
        RAISE EXCEPTION 'No geo_locations found — run V19/V24 migrations first';
    END IF;

    -- ── 2. Seed named facilities ──────────────────────────────────────
    -- Nyarugenge District Hospital
    SELECT id INTO v_fac_nyarugenge FROM facilities WHERE facility_code = 'KGL-NYR-DH-001';
    IF v_fac_nyarugenge IS NULL THEN
        INSERT INTO facilities (geo_location_id, name, facility_code, facility_type, district, phone, active)
        VALUES (v_geo_nyarugenge,
                'Nyarugenge District Hospital',
                'KGL-NYR-DH-001', 'DISTRICT_HOSPITAL', 'Nyarugenge', '+250788100001', TRUE)
        RETURNING id INTO v_fac_nyarugenge;
    END IF;

    -- Gasabo Health Centre
    SELECT id INTO v_fac_gasabo FROM facilities WHERE facility_code = 'KGL-GSB-HC-001';
    IF v_fac_gasabo IS NULL THEN
        INSERT INTO facilities (geo_location_id, name, facility_code, facility_type, district, phone, active)
        VALUES (v_geo_gasabo,
                'Gasabo Health Centre',
                'KGL-GSB-HC-001', 'HEALTH_CENTER', 'Gasabo', '+250788100002', TRUE)
        RETURNING id INTO v_fac_gasabo;
    END IF;

    -- Kicukiro Health Centre
    SELECT id INTO v_fac_kicukiro FROM facilities WHERE facility_code = 'KGL-KCK-HC-001';
    IF v_fac_kicukiro IS NULL THEN
        INSERT INTO facilities (geo_location_id, name, facility_code, facility_type, district, phone, active)
        VALUES (v_geo_kicukiro,
                'Kicukiro Health Centre',
                'KGL-KCK-HC-001', 'HEALTH_CENTER', 'Kicukiro', '+250788100003', TRUE)
        RETURNING id INTO v_fac_kicukiro;
    END IF;

    -- Rwamagana District Hospital
    SELECT id INTO v_fac_rwamagana FROM facilities WHERE facility_code = 'EST-RWM-DH-001';
    IF v_fac_rwamagana IS NULL THEN
        INSERT INTO facilities (geo_location_id, name, facility_code, facility_type, district, phone, active)
        VALUES (v_geo_rwamagana,
                'Rwamagana District Hospital',
                'EST-RWM-DH-001', 'DISTRICT_HOSPITAL', 'Rwamagana', '+250788100004', TRUE)
        RETURNING id INTO v_fac_rwamagana;
    END IF;

    -- Huye District Hospital
    SELECT id INTO v_fac_huye FROM facilities WHERE facility_code = 'SOU-HUY-DH-001';
    IF v_fac_huye IS NULL THEN
        INSERT INTO facilities (geo_location_id, name, facility_code, facility_type, district, phone, active)
        VALUES (v_geo_huye,
                'Huye District Hospital',
                'SOU-HUY-DH-001', 'DISTRICT_HOSPITAL', 'Huye', '+250788100005', TRUE)
        RETURNING id INTO v_fac_huye;
    END IF;

    -- ── 3. MOH_ADMIN ─────────────────────────────────────────────────
    -- Dr. Jean-Baptiste Habimana — system-wide administrator
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1198001000000001') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_nyarugenge, v_geo_nyarugenge,
                '1198001000000001', '+250788900001',
                v_pw_hash, 'MOH_ADMIN', 'Jean-Baptiste', 'Habimana',
                'en', TRUE)
        RETURNING id INTO v_uid_moh;

        -- MOH_ADMIN is also a government user
        INSERT INTO government_users (user_id, gov_role, ministry, employee_id, can_export, can_push_hmis)
        VALUES (v_uid_moh, 'MOH_ADMIN', 'Ministry of Health', 'MOH-EMP-000001', TRUE, TRUE);
    END IF;

    -- ── 4. FACILITY_ADMINs ────────────────────────────────────────────
    -- Immaculée Uwimana — Nyarugenge District Hospital admin
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1198501000000002') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_nyarugenge, v_geo_nyarugenge,
                '1198501000000002', '+250788900002',
                v_pw_hash, 'FACILITY_ADMIN', 'Immaculée', 'Uwimana',
                'rw', TRUE)
        RETURNING id INTO v_uid_fadm1;
    END IF;

    -- Patrick Nzeyimana — Gasabo Health Centre admin
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1199001000000003') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_gasabo, v_geo_gasabo,
                '1199001000000003', '+250788900003',
                v_pw_hash, 'FACILITY_ADMIN', 'Patrick', 'Nzeyimana',
                'rw', TRUE)
        RETURNING id INTO v_uid_fadm2;
    END IF;

    -- Chantal Mukamana — Rwamagana District Hospital admin
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1198701000000004') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_rwamagana, v_geo_rwamagana,
                '1198701000000004', '+250788900004',
                v_pw_hash, 'FACILITY_ADMIN', 'Chantal', 'Mukamana',
                'rw', TRUE)
        RETURNING id INTO v_uid_fadm3;
    END IF;

    -- ── 5. DISTRICT_OFFICERs (+ government_users rows) ───────────────
    -- Alain Bizimana — Kigali City district officer
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1197501000000005') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (NULL, v_geo_nyarugenge,
                '1197501000000005', '+250788900005',
                v_pw_hash, 'DISTRICT_OFFICER', 'Alain', 'Bizimana',
                'rw', TRUE)
        RETURNING id INTO v_uid_dist1;

        INSERT INTO government_users (user_id, gov_role, ministry, employee_id,
                                      scoped_geo_ids, can_export, can_push_hmis)
        VALUES (v_uid_dist1, 'DISTRICT_OFFICER', 'Ministry of Health',
                'DIST-EMP-000001',
                ARRAY[v_geo_nyarugenge, v_geo_gasabo, v_geo_kicukiro],
                TRUE, FALSE);
    END IF;

    -- Sylvie Ingabire — Eastern Province district officer
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1197801000000006') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (NULL, v_geo_rwamagana,
                '1197801000000006', '+250788900006',
                v_pw_hash, 'DISTRICT_OFFICER', 'Sylvie', 'Ingabire',
                'rw', TRUE)
        RETURNING id INTO v_uid_dist2;

        INSERT INTO government_users (user_id, gov_role, ministry, employee_id,
                                      scoped_geo_ids, can_export, can_push_hmis)
        VALUES (v_uid_dist2, 'DISTRICT_OFFICER', 'Ministry of Health',
                'DIST-EMP-000002',
                ARRAY[v_geo_rwamagana],
                TRUE, FALSE);
    END IF;

    -- ── 6. HEALTH_WORKERs ─────────────────────────────────────────────
    -- Eugenie Mukeshimana — Nyarugenge District Hospital
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1199501000000007') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_nyarugenge, v_geo_nyarugenge,
                '1199501000000007', '+250788900007',
                v_pw_hash, 'HEALTH_WORKER', 'Eugenie', 'Mukeshimana',
                'rw', TRUE)
        RETURNING id INTO v_uid_hw1;
    END IF;

    -- Clément Hakizimana — Gasabo Health Centre
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1199201000000008') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_gasabo, v_geo_gasabo,
                '1199201000000008', '+250788900008',
                v_pw_hash, 'HEALTH_WORKER', 'Clément', 'Hakizimana',
                'rw', TRUE)
        RETURNING id INTO v_uid_hw2;
    END IF;

    -- Vestine Nyiraneza — Kicukiro Health Centre
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1199801000000009') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_kicukiro, v_geo_kicukiro,
                '1199801000000009', '+250788900009',
                v_pw_hash, 'HEALTH_WORKER', 'Vestine', 'Nyiraneza',
                'rw', TRUE)
        RETURNING id INTO v_uid_hw3;
    END IF;

    -- Janvier Tuyishime — Rwamagana District Hospital
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1199001000000010') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_rwamagana, v_geo_rwamagana,
                '1199001000000010', '+250788900010',
                v_pw_hash, 'HEALTH_WORKER', 'Janvier', 'Tuyishime',
                'rw', TRUE)
        RETURNING id INTO v_uid_hw4;
    END IF;

    -- ── 7. MOTHER users ───────────────────────────────────────────────
    -- Goretti Umurungi — Gasabo, active pregnancy
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1199601000000011') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_gasabo, v_geo_gasabo,
                '1199601000000011', '+250788900011',
                v_pw_hash, 'MOTHER', 'Goretti', 'Umurungi',
                'rw', TRUE)
        RETURNING id INTO v_uid_mom1;

        INSERT INTO mothers (user_id, facility_id, geo_location_id,
                             health_id, nida_verified_status,
                             date_of_birth, education_level, version)
        VALUES (v_uid_mom1, v_fac_gasabo, v_geo_gasabo,
                'MH-2026-000001', 'VERIFIED',
                DATE '1996-03-14', 'SECONDARY', 0)
        RETURNING id INTO v_mid1;

        -- active pregnancy (LMP ~8 weeks ago, EDD ~32 weeks from now)
        INSERT INTO pregnancies (mother_id, lmp_date, edd, status, gravida, para, version)
        VALUES (v_mid1,
                CURRENT_DATE - INTERVAL '56 days',
                CURRENT_DATE + INTERVAL '224 days',
                'ACTIVE', 1, 0, 0);
    END IF;

    -- Yvette Uwizeyimana — Nyarugenge, gravida 2 para 1
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1199501000000012') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_nyarugenge, v_geo_nyarugenge,
                '1199501000000012', '+250788900012',
                v_pw_hash, 'MOTHER', 'Yvette', 'Uwizeyimana',
                'rw', TRUE)
        RETURNING id INTO v_uid_mom2;

        INSERT INTO mothers (user_id, facility_id, geo_location_id,
                             health_id, nida_verified_status,
                             date_of_birth, education_level, version)
        VALUES (v_uid_mom2, v_fac_nyarugenge, v_geo_nyarugenge,
                'MH-2026-000002', 'VERIFIED',
                DATE '1995-07-22', 'TERTIARY', 0)
        RETURNING id INTO v_mid2;

        -- completed previous pregnancy
        INSERT INTO pregnancies (mother_id, lmp_date, edd, status, gravida, para, version)
        VALUES (v_mid2,
                DATE '2024-01-10', DATE '2024-10-17',
                'DELIVERED', 2, 1, 0);
    END IF;

    -- Pascaline Iradukunda — Rwamagana, no pregnancy yet
    IF NOT EXISTS (SELECT 1 FROM users WHERE national_id = '1199701000000013') THEN
        INSERT INTO users (facility_id, geo_location_id, national_id, phone_number,
                           password_hash, role, first_name, last_name,
                           preferred_language, active)
        VALUES (v_fac_rwamagana, v_geo_rwamagana,
                '1199701000000013', '+250788900013',
                v_pw_hash, 'MOTHER', 'Pascaline', 'Iradukunda',
                'rw', TRUE)
        RETURNING id INTO v_uid_mom3;

        INSERT INTO mothers (user_id, facility_id, geo_location_id,
                             health_id, nida_verified_status,
                             date_of_birth, education_level, version)
        VALUES (v_uid_mom3, v_fac_rwamagana, v_geo_rwamagana,
                'MH-2026-000003', 'PENDING',
                DATE '1997-11-05', 'SECONDARY', 0);
    END IF;

    RAISE NOTICE 'V27: Named test accounts and facilities seeded successfully.';

END $$;

-- ============================================================
-- V25__Seed_Mock_Test_Mothers.sql
-- Seeds 1 500 test mothers with realistic Rwanda National IDs
-- for use with the embedded NIDA / Irembo / HMIS mock controllers.
--
-- All national IDs start with '1' → mock NIDA returns MATCH.
-- IDs starting with '3' return PARTIAL_MATCH in the mock.
-- Password for every test user: Test@1234
-- Hash ($2b$10): $2b$10$Z8DKO3zJOHz/HNS8ndNADOxpCnyJT0M4EzE3yghGZiXzTY7hnahpu
-- ============================================================

DO $$
DECLARE
    -- reuse the first Kigali City / Gasabo geo record (seeded in V19/V24)
    v_geo_id   UUID;
    v_fac_id   UUID;

    -- first names pool (Rwandan female names)
    v_fnames   TEXT[] := ARRAY[
        'Mukamana','Uwimana','Nyirabera','Umurungi','Ingabire',
        'Uwizeyimana','Mukandayisenga','Nyiramana','Iradukunda','Umutoni',
        'Mukeshimana','Uwamahoro','Nyirahabimana','Umuhoza','Ishimwe',
        'Uwingabire','Nyiraneza','Umunezero','Mukagasana','Uwamwezi'
    ];
    -- last names pool
    v_lnames   TEXT[] := ARRAY[
        'Mugisha','Habimana','Nshimiyimana','Bizimana','Nzeyimana',
        'Uwizeye','Tuyishime','Kamanzi','Ngabonziza','Ndayisaba',
        'Hakizimana','Nduwimana','Gasana','Nizeyimana','Mutabazi',
        'Kayitesi','Nkurunziza','Habyarimana','Uwera','Niyibizi'
    ];
    v_langs    TEXT[] := ARRAY['rw','rw','rw','en','fr'];

    i          INT;
    v_nid      TEXT;
    v_phone    TEXT;
    v_fname    TEXT;
    v_lname    TEXT;
    v_lang     TEXT;
    v_dob      DATE;
    v_uid      UUID;
    v_mid      UUID;
    v_healthid TEXT;
    v_yr       INT;
    v_prov     INT;
BEGIN
    -- ── locate an existing geo record ─────────────────────────────
    SELECT id INTO v_geo_id
    FROM geo_locations
    WHERE province = 'Kigali City'
      AND district = 'Gasabo'
    LIMIT 1;

    IF v_geo_id IS NULL THEN
        -- fall back to any available geo location
        SELECT id INTO v_geo_id FROM geo_locations WHERE active = TRUE LIMIT 1;
    END IF;

    IF v_geo_id IS NULL THEN
        RAISE EXCEPTION 'No geo_locations found — run V19/V24 migrations first';
    END IF;

    -- ── create (or reuse) a mock test facility ─────────────────────
    SELECT id INTO v_fac_id
    FROM facilities
    WHERE facility_code = 'MOCK-TEST-001';

    IF v_fac_id IS NULL THEN
        INSERT INTO facilities (geo_location_id, name, facility_code, facility_type, district, phone, active)
        VALUES (v_geo_id, 'Mock Test Health Centre', 'MOCK-TEST-001',
                'HEALTH_CENTER', 'Gasabo', '+250788000000', TRUE)
        RETURNING id INTO v_fac_id;
    END IF;

    -- ── generate 1 500 test mothers ──────────────────────────────
    FOR i IN 1..1500 LOOP

        -- 16-digit Rwanda national ID:  1 + YYYY + PP + 9-digit index
        v_yr   := 1975 + (i % 30);                               -- birth years 1975-2004
        v_prov := 1    + (i % 5);                                -- province codes 1-5
        v_nid  := '1'
               || v_yr::TEXT
               || LPAD(v_prov::TEXT, 2, '0')
               || LPAD(i::TEXT, 9, '0');                         -- 1+4+2+9 = 16 digits

        -- skip if this NID already exists
        CONTINUE WHEN EXISTS (SELECT 1 FROM users WHERE national_id = v_nid);

        -- Rwanda mobile number (+25078XXXXXXX — 9 digits after country code)
        v_phone := '+25078' || LPAD(i::TEXT, 7, '0');

        -- skip if phone already taken
        CONTINUE WHEN EXISTS (SELECT 1 FROM users WHERE phone_number = v_phone);

        v_fname := v_fnames[1 + ((i - 1) % array_length(v_fnames, 1))];
        v_lname := v_lnames[1 + ((i - 1) % array_length(v_lnames, 1))];
        v_lang  := v_langs [1 + ((i - 1) % array_length(v_langs,  1))];
        v_dob   := DATE '1975-01-01' + ((v_yr - 1975) * 365 + (i % 300)) * INTERVAL '1 day';

        -- health ID in standard MH-YYYY-NNNNNN format
        v_healthid := 'MH-' || v_yr::TEXT || '-' || LPAD(i::TEXT, 6, '0');

        -- ── user row ────────────────────────────────────────────────
        -- password hash = BCrypt10("Test@1234")
        INSERT INTO users (
            facility_id, geo_location_id, national_id, phone_number,
            password_hash, role, first_name, last_name,
            preferred_language, active
        )
        VALUES (
            v_fac_id, v_geo_id, v_nid, v_phone,
            '$2b$10$Z8DKO3zJOHz/HNS8ndNADOxpCnyJT0M4EzE3yghGZiXzTY7hnahpu',
            'HEALTH_WORKER', v_fname, v_lname,
            v_lang, TRUE
        )
        RETURNING id INTO v_uid;

        -- ── mother row ──────────────────────────────────────────────
        INSERT INTO mothers (
            user_id, facility_id, geo_location_id,
            health_id, nida_verified_status,
            date_of_birth, education_level,
            version
        )
        VALUES (
            v_uid, v_fac_id, v_geo_id,
            v_healthid, 'PENDING',
            v_dob, 'SECONDARY',
            0
        )
        RETURNING id INTO v_mid;

    END LOOP;

    -- ── bump the health-ID sequence so the application never collides ──
    PERFORM setval('seq_mother_health_id', 2000, TRUE);

    RAISE NOTICE 'Mock test mothers seeded successfully.';
END $$;

-- ============================================================
-- V18__Seed_EPI_Schedule.sql
-- Seeds the Rwanda EPI vaccination schedule.
-- Source: WHO/Rwanda MoH Expanded Programme on Immunization.
-- Idempotent: skips inserts that already exist by antigen_code.
-- ============================================================

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'BCG',                'BCG',        1,    0,  7, TRUE, 'Bacillus Calmette-Guerin - tuberculosis prevention'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'BCG');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'OPV-0 (Birth)',      'OPV0',       1,    0,  7, TRUE, 'Oral polio vaccine, birth dose'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'OPV0');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'Pentavalent-1',      'PENTA1',     1,   42, 14, TRUE, 'DTP-HepB-Hib first dose, 6 weeks'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'PENTA1');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'OPV-1',              'OPV1',       2,   42, 14, TRUE, 'Oral polio vaccine, 1st dose'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'OPV1');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'PCV-1',              'PCV1',       1,   42, 14, TRUE, 'Pneumococcal conjugate, 1st dose'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'PCV1');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'Rotavirus-1',        'ROTA1',      1,   42, 14, TRUE, 'Rotavirus vaccine, 1st dose'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'ROTA1');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'Pentavalent-2',      'PENTA2',     2,   70, 14, TRUE, 'DTP-HepB-Hib 2nd dose, 10 weeks'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'PENTA2');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'OPV-2',              'OPV2',       3,   70, 14, TRUE, 'Oral polio vaccine, 2nd dose'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'OPV2');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'PCV-2',              'PCV2',       2,   70, 14, TRUE, 'Pneumococcal conjugate, 2nd dose'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'PCV2');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'Rotavirus-2',        'ROTA2',      2,   70, 14, TRUE, 'Rotavirus vaccine, 2nd dose'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'ROTA2');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'Pentavalent-3',      'PENTA3',     3,   98, 14, TRUE, 'DTP-HepB-Hib 3rd dose, 14 weeks'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'PENTA3');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'OPV-3',              'OPV3',       4,   98, 14, TRUE, 'Oral polio vaccine, 3rd dose'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'OPV3');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'PCV-3',              'PCV3',       3,   98, 14, TRUE, 'Pneumococcal conjugate, 3rd dose'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'PCV3');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'IPV',                'IPV',        1,   98, 14, TRUE, 'Inactivated polio vaccine, 14 weeks'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'IPV');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'Measles-Rubella-1',  'MR1',        1,  273, 30, TRUE, 'Measles-Rubella 1st dose, 9 months'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'MR1');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'Yellow Fever',       'YF',         1,  273, 30, FALSE, 'Yellow fever, 9 months (selected districts)'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'YF');

INSERT INTO vaccination_schedules (id, vaccine_name, antigen_code, dose_number, due_age_days, window_days, is_mandatory, description)
SELECT gen_random_uuid(), 'Measles-Rubella-2',  'MR2',        2,  548, 60, TRUE, 'Measles-Rubella 2nd dose, 18 months'
WHERE NOT EXISTS (SELECT 1 FROM vaccination_schedules WHERE antigen_code = 'MR2');

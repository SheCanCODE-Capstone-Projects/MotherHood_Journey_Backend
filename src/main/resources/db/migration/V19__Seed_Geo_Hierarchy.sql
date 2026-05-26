-- ============================================================
-- V19__Seed_Geo_Hierarchy.sql
-- Seeds Rwanda administrative hierarchy down to the DISTRICT level.
--
-- IMPORTANT:
--   sector / cell / village are populated with the literal value 'UNASSIGNED'
--   as placeholders until the full MINALOC/MoH 14,000-row dataset is loaded.
--   The frontend should hide 'UNASSIGNED' rows once real sector/cell/village
--   data is imported.
--
-- Source: Government of Rwanda / MINALOC administrative divisions.
-- 5 provinces, 30 districts.
-- ============================================================

INSERT INTO geo_locations (province, district, sector, cell, village, active) VALUES

-- Kigali City (3 districts)
('Kigali City', 'Gasabo',      'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Kigali City', 'Kicukiro',    'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Kigali City', 'Nyarugenge',  'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),

-- Northern Province (5 districts)
('Northern',    'Burera',      'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Northern',    'Gakenke',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Northern',    'Gicumbi',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Northern',    'Musanze',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Northern',    'Rulindo',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),

-- Southern Province (8 districts)
('Southern',    'Gisagara',    'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Southern',    'Huye',        'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Southern',    'Kamonyi',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Southern',    'Muhanga',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Southern',    'Nyamagabe',   'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Southern',    'Nyanza',      'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Southern',    'Nyaruguru',   'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Southern',    'Ruhango',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),

-- Eastern Province (7 districts)
('Eastern',     'Bugesera',    'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Eastern',     'Gatsibo',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Eastern',     'Kayonza',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Eastern',     'Kirehe',      'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Eastern',     'Ngoma',       'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Eastern',     'Nyagatare',   'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Eastern',     'Rwamagana',   'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),

-- Western Province (7 districts)
('Western',     'Karongi',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Western',     'Ngororero',   'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Western',     'Nyabihu',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Western',     'Nyamasheke',  'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Western',     'Rubavu',      'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Western',     'Rusizi',      'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE),
('Western',     'Rutsiro',     'UNASSIGNED', 'UNASSIGNED', 'UNASSIGNED', TRUE);

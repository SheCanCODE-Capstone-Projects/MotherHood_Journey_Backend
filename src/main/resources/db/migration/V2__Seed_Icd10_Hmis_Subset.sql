-- ============================================================
-- V2__Seed_Icd10_Hmis_Subset.sql
-- Rwanda MoH HMIS ICD-10 reference table (Sprint 1 subset)
-- Used for server-side validation of diagnosis codes
-- ============================================================

CREATE TABLE IF NOT EXISTS icd10_codes (
    code        VARCHAR(8)   PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    category    VARCHAR(64)  NOT NULL
);

INSERT INTO icd10_codes (code, description, category) VALUES
-- Pregnancy & childbirth
('O00', 'Ectopic pregnancy', 'Pregnancy'),
('O10', 'Pre-existing hypertension complicating pregnancy', 'Pregnancy'),
('O11', 'Pre-existing hypertension with pre-eclampsia', 'Pregnancy'),
('O13', 'Gestational hypertension', 'Pregnancy'),
('O14', 'Pre-eclampsia', 'Pregnancy'),
('O15', 'Eclampsia', 'Pregnancy'),
('O20', 'Haemorrhage in early pregnancy', 'Pregnancy'),
('O23', 'Infections of genitourinary tract in pregnancy', 'Pregnancy'),
('O24', 'Diabetes mellitus in pregnancy', 'Pregnancy'),
('O36', 'Maternal care for other fetal problems', 'Pregnancy'),
('O42', 'Premature rupture of membranes', 'Pregnancy'),
('O60', 'Preterm labour', 'Pregnancy'),
('O72', 'Postpartum haemorrhage', 'Pregnancy'),
('O80', 'Single spontaneous delivery', 'Pregnancy'),
('O82', 'Single delivery by caesarean section', 'Pregnancy'),
('O85', 'Puerperal sepsis', 'Pregnancy'),
('O90', 'Complications of the puerperium', 'Pregnancy'),
-- Newborn / perinatal
('P07', 'Disorders related to short gestation and low birth weight', 'Perinatal'),
('P20', 'Intrauterine hypoxia', 'Perinatal'),
('P21', 'Birth asphyxia', 'Perinatal'),
('P22', 'Respiratory distress of newborn', 'Perinatal'),
('P36', 'Bacterial sepsis of newborn', 'Perinatal'),
('P55', 'Haemolytic disease of newborn', 'Perinatal'),
('P57', 'Kernicterus', 'Perinatal'),
('P59', 'Neonatal jaundice', 'Perinatal'),
('P92', 'Feeding problems of newborn', 'Perinatal'),
-- Malaria
('B50', 'Plasmodium falciparum malaria', 'Malaria'),
('B51', 'Plasmodium vivax malaria', 'Malaria'),
('B54', 'Unspecified malaria', 'Malaria'),
-- Anaemia
('D50', 'Iron deficiency anaemia', 'Anaemia'),
('D51', 'Vitamin B12 deficiency anaemia', 'Anaemia'),
('D53', 'Other nutritional anaemias', 'Anaemia'),
-- Malnutrition
('E40', 'Kwashiorkor', 'Malnutrition'),
('E41', 'Nutritional marasmus', 'Malnutrition'),
('E43', 'Unspecified severe protein-energy malnutrition', 'Malnutrition'),
('E46', 'Unspecified protein-energy malnutrition', 'Malnutrition'),
-- Respiratory
('J06', 'Acute upper respiratory infections', 'Respiratory'),
('J18', 'Pneumonia, unspecified', 'Respiratory'),
('J21', 'Acute bronchiolitis', 'Respiratory'),
-- Diarrhoea
('A09', 'Diarrhoea and gastroenteritis of presumed infectious origin', 'Diarrhoea'),
-- HIV
('B20', 'HIV disease resulting in infectious and parasitic diseases', 'HIV'),
('B24', 'Unspecified HIV disease', 'HIV'),
-- Hypertension
('I10', 'Essential (primary) hypertension', 'Hypertension'),
-- Diabetes
('E11', 'Type 2 diabetes mellitus', 'Diabetes'),
('E14', 'Unspecified diabetes mellitus', 'Diabetes')
ON CONFLICT (code) DO NOTHING;

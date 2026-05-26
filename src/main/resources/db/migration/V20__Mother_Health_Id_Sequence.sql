-- ============================================================
-- V20__Mother_Health_Id_Sequence.sql
-- Creates the sequence used by MotherService.generateHealthId() to
-- produce the MH-YYYY-NNNNNN health ID format.
-- Tracks the README's "Known Issue #1" — previously missing from Flyway.
-- ============================================================

CREATE SEQUENCE IF NOT EXISTS seq_mother_health_id START 1 INCREMENT 1;

-- ============================================================
-- V6__SR_Sequence.sql
-- Replace count-based SR reference generation with a DB sequence.
-- Eliminates the race condition in ServiceRequestServiceImpl.
-- Thread-safe across all app instances and restarts.
-- ============================================================

CREATE SEQUENCE IF NOT EXISTS sr_ref_seq
    START 1
    INCREMENT 1
    NO CYCLE;

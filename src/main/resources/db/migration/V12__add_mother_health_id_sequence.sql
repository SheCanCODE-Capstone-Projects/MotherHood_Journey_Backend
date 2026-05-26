-- Sequence used by MotherService.generateHealthId() to produce MH-YYYY-NNNNNN health IDs.
-- Must be present on every fresh deployment; this was missing from V1–V11.
CREATE SEQUENCE IF NOT EXISTS seq_mother_health_id
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 1;
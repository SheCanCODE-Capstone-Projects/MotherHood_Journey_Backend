-- ============================================================
-- V26__Performance_And_Delivery_Status.sql
-- 1. Index on at_message_id for O(1) webhook lookups.
-- 2. Index on notification_type + status for queue reports.
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_sms_at_message_id
    ON sms_notifications (at_message_id)
    WHERE at_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sms_type_status
    ON sms_notifications (notification_type, status);

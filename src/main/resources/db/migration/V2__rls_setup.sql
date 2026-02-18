-- ============================================================================
-- VetClinic - V2: Row-Level Security Setup
-- Run after V1 - requires vetapp_user role to exist
-- ============================================================================

-- 1. Application-level DB user (non-superuser, RLS applies)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'vetapp_user') THEN
        CREATE ROLE vetapp_user WITH LOGIN PASSWORD 'vetapp_pass';
    END IF;
END $$;

GRANT CONNECT ON DATABASE vetapp TO vetapp_user;
GRANT USAGE ON SCHEMA public TO vetapp_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO vetapp_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO vetapp_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO vetapp_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO vetapp_user;

-- 2. Enable RLS on all tenant-scoped tables
ALTER TABLE clinic_location ENABLE ROW LEVEL SECURITY;
ALTER TABLE role ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_location ENABLE ROW LEVEL SECURITY;
ALTER TABLE species ENABLE ROW LEVEL SECURITY;
ALTER TABLE breed ENABLE ROW LEVEL SECURITY;
ALTER TABLE owner ENABLE ROW LEVEL SECURITY;
ALTER TABLE pet ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointment ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointment_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE medical_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE service ENABLE ROW LEVEL SECURITY;
ALTER TABLE treatment ENABLE ROW LEVEL SECURITY;
ALTER TABLE vaccination ENABLE ROW LEVEL SECURITY;
ALTER TABLE prescription ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_transaction ENABLE ROW LEVEL SECURITY;
ALTER TABLE document ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE clinic ENABLE ROW LEVEL SECURITY;

-- refresh_token: NO RLS (no clinic_id, accessed by token string)

-- 3. RLS policies
-- NULLIF handles PostgreSQL versions where current_setting returns '' instead of NULL
-- Unset variable -> '' -> NULL -> clinic_id = NULL -> false -> no rows (fail-safe)

CREATE POLICY tenant_isolation ON clinic_location
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON role
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON users
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON user_location
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON species
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON breed
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON owner
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON pet
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON appointment
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON appointment_status_history
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON medical_record
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON service
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON treatment
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON vaccination
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON prescription
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON invoice
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON invoice_item
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON payment
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON inventory_item
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON inventory_transaction
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON document
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON notification
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

CREATE POLICY tenant_isolation ON audit_log
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

-- Clinic table: uses 'id' instead of 'clinic_id' (it IS the clinic)
CREATE POLICY tenant_isolation ON clinic
    FOR ALL USING (id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

-- 4. SECURITY DEFINER helper functions (bypass RLS for auth flows)

CREATE OR REPLACE FUNCTION get_clinic_id_for_user(p_user_id UUID)
RETURNS UUID AS $$
    SELECT clinic_id FROM users WHERE id = p_user_id;
$$ LANGUAGE SQL SECURITY DEFINER STABLE;

CREATE OR REPLACE FUNCTION count_all_users()
RETURNS BIGINT AS $$
    SELECT COUNT(*) FROM users;
$$ LANGUAGE SQL SECURITY DEFINER STABLE;

GRANT EXECUTE ON FUNCTION get_clinic_id_for_user(UUID) TO vetapp_user;
GRANT EXECUTE ON FUNCTION count_all_users() TO vetapp_user;

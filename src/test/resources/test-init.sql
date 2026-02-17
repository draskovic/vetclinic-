-- Functions required by AuthService and DataSeeder
-- Using single-quote syntax instead of $$ to avoid Spring @Sql delimiter issues

CREATE OR REPLACE FUNCTION get_clinic_id_for_user(p_user_id UUID)
RETURNS UUID AS 'SELECT clinic_id FROM users WHERE id = p_user_id' LANGUAGE SQL STABLE;

CREATE OR REPLACE FUNCTION count_all_users()
RETURNS BIGINT AS 'SELECT COUNT(*) FROM users' LANGUAGE SQL STABLE;

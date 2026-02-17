-- Seed test data for VetClinic Security testing
-- Run this manually: psql -U postgres -d vetapp -f seed-test-data.sql

-- 1. Create test clinic
INSERT INTO clinic (id, name, email, phone, city, country, subscription_plan, active, settings, created_at, updated_at, deleted, version)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Test Vet Clinic',
    'clinic@test.com',
    '+381111234567',
    'Beograd',
    'Serbia',
    'PROFESSIONAL',
    true,
    '{}',
    NOW(), NOW(), false, 0
) ON CONFLICT (id) DO NOTHING;

-- 2. Create ADMIN role
INSERT INTO role (id, clinic_id, name, permissions, created_at, updated_at, deleted, version)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'ADMIN',
    '["*"]',
    NOW(), NOW(), false, 0
) ON CONFLICT DO NOTHING;

-- 3. Create test user (password: admin123, BCrypt hash)
-- BCrypt hash of "admin123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (id, clinic_id, role_id, first_name, last_name, email, password_hash, active, created_at, updated_at, deleted, version)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'Admin',
    'User',
    'admin@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    true,
    NOW(), NOW(), false, 0
) ON CONFLICT DO NOTHING;

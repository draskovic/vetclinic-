-- ============================================================
-- VetClinic Demo Seed Data
-- Realistic Serbian data for client demonstration
-- Run in pgAdmin: Open Query Tool > Open File > Execute (F5)
-- ============================================================
-- Clinic: "Test Vet Clinic" (b5434818-265f-4386-8ed5-e568a238a451)
-- Existing data: 1 location, 8 species, 19+ breeds, 2 owners,
--                3 pets, 17 services, 1 admin, 1 vet
-- This script ADDS: 3 vets, 8 owners, 11 pets, 14 appointments,
--   6 medical records, 6 vaccinations, 7 invoices, 5 prescriptions,
--   14 inventory items, 8 inventory transactions
-- ============================================================

DO $$
DECLARE
    -- Existing IDs
    v_clinic UUID := 'b5434818-265f-4386-8ed5-e568a238a451';
        v_admin UUID := '963b228a-90ce-4b26-8ac5-8b46b6cc1dd6';

    v_vet_role UUID := 'f1e746f4-e14e-411e-bcda-5e0e993643b3';
    v_location UUID := '4d1233d9-415f-4946-98a3-ecd635f9a66f';

    -- Existing species
    v_pas UUID := 'f9940998-7a36-49ec-bd10-49ca19f92ce9';
    v_macka UUID := '83f3bb63-3202-4d30-ad40-2cbbe6adc688';
    v_zec UUID := 'edde8d6a-d154-4b80-af1d-595abb9c5ded';
    v_ptica UUID := '0a8b0275-27b0-453a-a1e3-ac36261600b9';
    v_hrcak UUID := '71806e9b-1aa6-4d45-8ac2-003148e51b1f';

    -- Existing owners
    v_owner_nenad UUID := '872899a9-da02-48c7-b1d6-d01045b1e42e';
    v_owner_pera UUID := 'e67419ad-1330-4df6-b645-27810b3ea027';

    -- Existing pets
    v_pet_nora UUID := '703f6231-da15-48c2-a576-e000e7f3cc28';
    v_pet_rio UUID := 'd3a3374f-8609-4080-9fb9-c2e3afeeebda';
    v_pet_rex UUID := 'fdd0a20d-0a04-4284-835c-e6de5d06e278';

    -- Existing breeds
    v_breed_haski UUID := 'f873e6c3-ec83-4009-ab02-a6dbaf62fb1a';
    v_breed_doberman UUID := '1f9d6fc8-b22b-4082-8ed9-68d676725822';
    v_breed_bigl UUID := '2a3a2801-644f-41d8-960c-81d33e7568ad';
    v_breed_persijska UUID := '3cf39151-f488-4b99-bbc0-6cbc0e416298';
    v_breed_sijamska UUID := '1ea2cdc8-f323-4a53-a3e7-2c1b389dd3b5';
    v_breed_britanska UUID := '0ed55da6-66aa-4627-9fb7-bbb1f75898c5';
    v_breed_maine_coon UUID := 'fdf68937-535c-45a0-b908-15d3c82c1854';
    v_breed_ragdoll UUID := 'ec1699e0-a0b4-4a0e-a775-a8963f4c4933';
    v_breed_jorksirac UUID := 'a44b40f8-16eb-442a-92ae-8fb6cfbb64af';
    v_breed_rotvajler UUID := 'b73ffa3f-c226-4aa5-bbee-2701320ad483';
    v_breed_srednja_pudla UUID := '4e7db23c-fd99-4368-9a0c-188dab834314';

    -- New vet IDs
    v_vet_marko UUID := 'c0000000-0000-0000-0000-000000000002';
    v_vet_ana UUID := 'c0000000-0000-0000-0000-000000000003';
    v_vet_nikola UUID := 'c0000000-0000-0000-0000-000000000004';

    -- New owner IDs
    v_owner_stefan UUID := '10000000-0000-0000-0000-000000000003';
    v_owner_jelena UUID := '10000000-0000-0000-0000-000000000004';
    v_owner_dragan UUID := '10000000-0000-0000-0000-000000000005';
    v_owner_ivana UUID := '10000000-0000-0000-0000-000000000006';
    v_owner_nemanja UUID := '10000000-0000-0000-0000-000000000007';
    v_owner_maja UUID := '10000000-0000-0000-0000-000000000008';
    v_owner_aleksandar UUID := '10000000-0000-0000-0000-000000000009';
    v_owner_tamara UUID := '10000000-0000-0000-0000-000000000010';

    -- New pet IDs
    v_pet_badi UUID := '20000000-0000-0000-0000-000000000003';
    v_pet_kiki UUID := '20000000-0000-0000-0000-000000000004';
    v_pet_mika UUID := '20000000-0000-0000-0000-000000000005';
    v_pet_carli UUID := '20000000-0000-0000-0000-000000000006';
    v_pet_bella UUID := '20000000-0000-0000-0000-000000000007';
    v_pet_sima UUID := '20000000-0000-0000-0000-000000000008';
    v_pet_kleopatra UUID := '20000000-0000-0000-0000-000000000009';
    v_pet_zeka UUID := '20000000-0000-0000-0000-000000000010';
    v_pet_roksi UUID := '20000000-0000-0000-0000-000000000011';
    v_pet_grom UUID := '20000000-0000-0000-0000-000000000012';
    v_pet_lola UUID := '20000000-0000-0000-0000-000000000013';

BEGIN

-- ============================================================
-- 1. VET USERS (VET role already exists)
-- Password: admin123 (same BCrypt hash as admin user)
-- ============================================================
INSERT INTO users (id, clinic_id, role_id, first_name, last_name, email, password_hash, phone, license_number, specialization, active, created_at, updated_at, deleted, version)
VALUES
    (v_vet_marko, v_clinic, v_vet_role,
     'Marko', 'Petrović', 'marko.petrovic@vetclinic.rs',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     '+381641234567', 'VS-2024-001', 'Hirurgija',
     true, NOW(), NOW(), false, 0),
    (v_vet_ana, v_clinic, v_vet_role,
     'Ana', 'Jovanović', 'ana.jovanovic@vetclinic.rs',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     '+381649876543', 'VS-2024-002', 'Dermatologija',
     true, NOW(), NOW(), false, 0),
    (v_vet_nikola, v_clinic, v_vet_role,
     'Nikola', 'Ilić', 'nikola.ilic@vetclinic.rs',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     '+381652223344', 'VS-2024-003', 'Stomatologija',
     true, NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. OWNERS (8 new + 2 existing = 10 total)
-- ============================================================
INSERT INTO owner (id, clinic_id, first_name, last_name, email, phone, address, city, note, created_at, updated_at, deleted, version)
VALUES
    (v_owner_stefan, v_clinic, 'Stefan', 'Stojanović', 'stefan.stojanovic@gmail.com', '+381641003003', 'Futoška 100', 'Novi Sad', 'Vlasnik dva psa. Uvek plaća na vreme.', NOW(), NOW(), false, 0),
    (v_owner_jelena, v_clinic, 'Jelena', 'Marković', 'jelena.markovic@hotmail.com', '+381641004004', 'Kisačka 33', 'Beograd', NULL, NOW(), NOW(), false, 0),
    (v_owner_dragan, v_clinic, 'Dragan', 'Pavlović', 'dragan.pavlovic@gmail.com', '+381641005005', 'Rumenačka 78', 'Novi Sad', 'Preporučio nas prijateljima.', NOW(), NOW(), false, 0),
    (v_owner_ivana, v_clinic, 'Ivana', 'Janković', 'ivana.jankovic@gmail.com', '+381641006006', 'Šafarikova 5', 'Beograd', NULL, NOW(), NOW(), false, 0),
    (v_owner_nemanja, v_clinic, 'Nemanja', 'Tomić', 'nemanja.tomic@yahoo.com', '+381641007007', 'Narodnog fronta 22', 'Novi Sad', 'Vlasnik mačke i zeca.', NOW(), NOW(), false, 0),
    (v_owner_maja, v_clinic, 'Maja', 'Vasić', 'maja.vasic@gmail.com', '+381641008008', 'Laze Telečkog 8', 'Beograd', NULL, NOW(), NOW(), false, 0),
    (v_owner_aleksandar, v_clinic, 'Aleksandar', 'Ristić', 'aleksandar.ristic@gmail.com', '+381641009009', 'Jovana Subotića 15', 'Novi Sad', 'Veliki ljubitelj životinja.', NOW(), NOW(), false, 0),
    (v_owner_tamara, v_clinic, 'Tamara', 'Popović', 'tamara.popovic@hotmail.com', '+381641010010', 'Cara Dušana 44', 'Beograd', NULL, NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. PETS (11 new + 3 existing = 14 total)
-- ============================================================
INSERT INTO pet (id, clinic_id, owner_id, species_id, breed_id, name, date_of_birth, gender, color, weight_kg, microchip_number, is_neutered, is_deceased, allergies, note, created_at, updated_at, deleted, version)
VALUES
    -- Stefan Stojanović - 2 dogs
    (v_pet_badi, v_clinic, v_owner_stefan, v_pas, v_breed_doberman,
     'Badi', '2020-01-10', 'MALE', 'Crno-smeđa', 38.0, '688000003456789', false, false,
     NULL, 'Doberman, čuvar dvorišta.', NOW(), NOW(), false, 0),
    (v_pet_kiki, v_clinic, v_owner_stefan, v_pas, v_breed_jorksirac,
     'Kiki', '2023-05-01', 'FEMALE', 'Zlatno-siva', 3.2, '688000004567890', false, false,
     NULL, 'Jorkširski terijer, mala ali energična.', NOW(), NOW(), false, 0),

    -- Jelena Marković - 1 cat
    (v_pet_mika, v_clinic, v_owner_jelena, v_macka, v_breed_maine_coon,
     'Mika', '2021-09-12', 'MALE', 'Siva tabby', 7.5, '688000005678901', true, false,
     NULL, 'Mejn kun, jako krupan.', NOW(), NOW(), false, 0),

    -- Dragan Pavlović - 1 dog
    (v_pet_carli, v_clinic, v_owner_dragan, v_pas, v_breed_bigl,
     'Čarli', '2022-02-28', 'MALE', 'Tricolor', 12.3, '688000006789012', false, false,
     'Alergija na buvu', 'Veoma druželjubiv bigl.', NOW(), NOW(), false, 0),

    -- Ivana Janković - 2 cats
    (v_pet_bella, v_clinic, v_owner_ivana, v_macka, v_breed_britanska,
     'Bella', '2023-01-15', 'FEMALE', 'Siva', 5.1, '688000007890123', false, false,
     NULL, 'Britanska kratkodlaka, mirna narav.', NOW(), NOW(), false, 0),
    (v_pet_sima, v_clinic, v_owner_ivana, v_macka, v_breed_sijamska,
     'Sima', '2023-07-01', 'MALE', 'Seal point', 4.0, '688000008901234', false, false,
     NULL, 'Sijamski mačak, veoma vokalan.', NOW(), NOW(), false, 0),

    -- Nemanja Tomić - 1 cat + 1 rabbit
    (v_pet_kleopatra, v_clinic, v_owner_nemanja, v_macka, v_breed_ragdoll,
     'Kleopatra', '2022-11-05', 'FEMALE', 'Belo-siva', 5.8, '688000009012345', true, false,
     NULL, 'Ragdol mačka, voli društvo.', NOW(), NOW(), false, 0),
    (v_pet_zeka, v_clinic, v_owner_nemanja, v_zec, NULL,
     'Zeka', '2024-02-14', 'MALE', 'Bela', 1.8, NULL, false, false,
     NULL, 'Patuljasti zec.', NOW(), NOW(), false, 0),

    -- Maja Vasić - 1 dog
    (v_pet_roksi, v_clinic, v_owner_maja, v_pas, v_breed_rotvajler,
     'Roksi', '2023-08-20', 'FEMALE', 'Crno-smeđa', 32.5, '688000010123456', false, false,
     NULL, 'Rotvajler. Izuzetno poslušna.', NOW(), NOW(), false, 0),

    -- Aleksandar Ristić - 2 dogs
    (v_pet_grom, v_clinic, v_owner_aleksandar, v_pas, v_breed_haski,
     'Grom', '2019-07-04', 'MALE', 'Sivo-bela', 25.0, '688000011234567', false, false,
     NULL, 'Haski, voli hladnoću.', NOW(), NOW(), false, 0),
    (v_pet_lola, v_clinic, v_owner_aleksandar, v_pas, v_breed_srednja_pudla,
     'Lola', '2024-01-10', 'FEMALE', 'Crna', 6.5, '688000012345678', false, false,
     NULL, 'Pudla štene, u procesu vakcinacije.', NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. APPOINTMENTS (past, today, future)
-- ============================================================
INSERT INTO appointment (id, clinic_id, location_id, pet_id, owner_id, vet_id, start_time, end_time, status, type, reason, notes, created_at, updated_at, deleted, version)
VALUES
    -- Past (completed)
    ('40000000-0000-0000-0000-000000000001', v_clinic, v_location,
     v_pet_nora, v_owner_nenad, v_vet_marko,
     (CURRENT_DATE - INTERVAL '14 days') + TIME '09:00', (CURRENT_DATE - INTERVAL '14 days') + TIME '09:30',
     'COMPLETED', 'CHECKUP', 'Redovni godišnji pregled', 'Pas u odličnom stanju.', NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000002', v_clinic, v_location,
     v_pet_bella, v_owner_ivana, v_vet_ana,
     (CURRENT_DATE - INTERVAL '10 days') + TIME '10:00', (CURRENT_DATE - INTERVAL '10 days') + TIME '10:30',
     'COMPLETED', 'VACCINATION', 'Godišnja vakcinacija', 'Primljena tricat vakcina.', NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000003', v_clinic, v_location,
     v_pet_badi, v_owner_stefan, v_vet_marko,
     (CURRENT_DATE - INTERVAL '7 days') + TIME '11:00', (CURRENT_DATE - INTERVAL '7 days') + TIME '12:00',
     'COMPLETED', 'SURGERY', 'Kastracija', 'Operacija protekla bez komplikacija.', NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000004', v_clinic, v_location,
     v_pet_mika, v_owner_jelena, v_vet_ana,
     (CURRENT_DATE - INTERVAL '5 days') + TIME '14:00', (CURRENT_DATE - INTERVAL '5 days') + TIME '14:45',
     'COMPLETED', 'CHECKUP', 'Problem sa kožom', 'Dijagnoza: atopijski dermatitis.', NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000005', v_clinic, v_location,
     v_pet_carli, v_owner_dragan, v_vet_marko,
     (CURRENT_DATE - INTERVAL '3 days') + TIME '09:30', (CURRENT_DATE - INTERVAL '3 days') + TIME '10:15',
     'COMPLETED', 'CHECKUP', 'Povraćanje i gubitak apetita', 'Gastritis.', NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000006', v_clinic, v_location,
     v_pet_roksi, v_owner_maja, v_vet_nikola,
     (CURRENT_DATE - INTERVAL '2 days') + TIME '15:00', (CURRENT_DATE - INTERVAL '2 days') + TIME '15:45',
     'COMPLETED', 'SURGERY', 'Loš zadah', 'Očišćen kamenac. Izvađen jedan zub.', NOW(), NOW(), false, 0),

    -- Today
    ('40000000-0000-0000-0000-000000000007', v_clinic, v_location,
     v_pet_rio, v_owner_nenad, v_vet_marko,
     CURRENT_DATE + TIME '09:00', CURRENT_DATE + TIME '09:30',
     'SCHEDULED', 'VACCINATION', 'Revakcinacija - besnilo', NULL, NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000008', v_clinic, v_location,
     v_pet_kiki, v_owner_stefan, v_vet_ana,
     CURRENT_DATE + TIME '10:00', CURRENT_DATE + TIME '10:30',
     'CONFIRMED', 'CHECKUP', 'Vakcinacija šteneta', NULL, NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000009', v_clinic, v_location,
     v_pet_sima, v_owner_ivana, v_vet_ana,
     CURRENT_DATE + TIME '11:30', CURRENT_DATE + TIME '12:00',
     'SCHEDULED', 'CHECKUP', 'Redovni pregled', NULL, NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000010', v_clinic, v_location,
     v_pet_grom, v_owner_aleksandar, v_vet_marko,
     CURRENT_DATE + TIME '14:00', CURRENT_DATE + TIME '14:30',
     'SCHEDULED', 'FOLLOW_UP', 'Kontrola nakon terapije', NULL, NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000011', v_clinic, v_location,
     v_pet_lola, v_owner_aleksandar, v_vet_marko,
     CURRENT_DATE + TIME '15:00', CURRENT_DATE + TIME '15:30',
     'SCHEDULED', 'VACCINATION', 'Štenećak - prva doza', NULL, NOW(), NOW(), false, 0),

    -- Future
    ('40000000-0000-0000-0000-000000000012', v_clinic, v_location,
     v_pet_kleopatra, v_owner_nemanja, v_vet_ana,
     (CURRENT_DATE + INTERVAL '1 day') + TIME '09:30', (CURRENT_DATE + INTERVAL '1 day') + TIME '10:00',
     'SCHEDULED', 'CHECKUP', 'Pregled - gubitak dlake', NULL, NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000013', v_clinic, v_location,
     v_pet_badi, v_owner_stefan, v_vet_marko,
     (CURRENT_DATE + INTERVAL '2 days') + TIME '11:00', (CURRENT_DATE + INTERVAL '2 days') + TIME '11:30',
     'SCHEDULED', 'FOLLOW_UP', 'Kontrola nakon kastracije', NULL, NOW(), NOW(), false, 0),

    ('40000000-0000-0000-0000-000000000014', v_clinic, v_location,
     v_pet_rex, v_owner_pera, v_vet_nikola,
     (CURRENT_DATE + INTERVAL '3 days') + TIME '13:00', (CURRENT_DATE + INTERVAL '3 days') + TIME '13:30',
     'SCHEDULED', 'CHECKUP', 'Redovni pregled', NULL, NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 5. MEDICAL RECORDS
-- ============================================================
INSERT INTO medical_record (id, clinic_id, appointment_id, pet_id, vet_id, symptoms, diagnosis, examination_notes, weight_kg, temperature_c, heart_rate, follow_up_recommended, follow_up_date, created_at, updated_at, deleted, version)
VALUES
    ('50000000-0000-0000-0000-000000000001', v_clinic, '40000000-0000-0000-0000-000000000001',
     v_pet_nora, v_vet_marko,
     'Nema simptoma, redovni pregled', 'Zdrav pas, bez patoloških promena',
     'Sluzokoža roze, turgor kože uredan, auskultacija uredna.',
     28.0, 38.6, 80, false, NULL, NOW(), NOW(), false, 0),

    ('50000000-0000-0000-0000-000000000002', v_clinic, '40000000-0000-0000-0000-000000000002',
     v_pet_bella, v_vet_ana,
     'Nema simptoma, vakcinacija', 'Zdrava mačka, vakcinisana',
     'Primenjena Nobivac Tricat Trio vakcina.',
     5.1, 38.4, 140, false, NULL, NOW(), NOW(), false, 0),

    ('50000000-0000-0000-0000-000000000003', v_clinic, '40000000-0000-0000-0000-000000000003',
     v_pet_badi, v_vet_marko,
     'Kastracija', 'Orchiectomia - uspešno',
     'Anestezija: Zoletil + Xylazin. Operacija uredno.',
     38.0, 38.8, 90, true, CURRENT_DATE + INTERVAL '7 days', NOW(), NOW(), false, 0),

    ('50000000-0000-0000-0000-000000000004', v_clinic, '40000000-0000-0000-0000-000000000004',
     v_pet_mika, v_vet_ana,
     'Češanje, crvenilo kože, gubitak dlake', 'Atopijski dermatitis',
     'Promene na koži trbuha i pazuha.',
     7.5, 38.9, 120, true, CURRENT_DATE + INTERVAL '14 days', NOW(), NOW(), false, 0),

    ('50000000-0000-0000-0000-000000000005', v_clinic, '40000000-0000-0000-0000-000000000005',
     v_pet_carli, v_vet_marko,
     'Povraćanje 3 dana, ne jede', 'Akutni gastritis',
     'Dehidratacija prisutna. Infuzija NaCl 250ml.',
     11.8, 39.2, 100, true, CURRENT_DATE + INTERVAL '5 days', NOW(), NOW(), false, 0),

    ('50000000-0000-0000-0000-000000000006', v_clinic, '40000000-0000-0000-0000-000000000006',
     v_pet_roksi, v_vet_nikola,
     'Loš zadah, otežano žvakanje', 'Parodontalna bolest, fraktura zuba',
     'Čišćenje kamenca. Ekstrakcija zuba 108.',
     32.0, 38.5, 88, true, CURRENT_DATE + INTERVAL '10 days', NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 6. VACCINATIONS (due dates for dashboard)
-- ============================================================
INSERT INTO vaccination (id, clinic_id, pet_id, medical_record_id, vet_id, vaccine_name, batch_number, manufacturer, administered_at, valid_until, next_due_date, created_at, updated_at, deleted, version)
VALUES
    ('60000000-0000-0000-0000-000000000001', v_clinic, v_pet_rio, NULL,
     v_vet_marko, 'Nobivac Rabies', 'A1234-25', 'MSD Animal Health',
     (CURRENT_DATE - INTERVAL '14 days') + TIME '09:15', CURRENT_DATE + INTERVAL '1 year',
     CURRENT_DATE + INTERVAL '5 days', NOW(), NOW(), false, 0),

    ('60000000-0000-0000-0000-000000000002', v_clinic, v_pet_bella, '50000000-0000-0000-0000-000000000002',
     v_vet_ana, 'Nobivac Tricat Trio', 'B5678-25', 'MSD Animal Health',
     (CURRENT_DATE - INTERVAL '10 days') + TIME '10:15', CURRENT_DATE + INTERVAL '1 year',
     CURRENT_DATE + INTERVAL '3 days', NOW(), NOW(), false, 0),

    ('60000000-0000-0000-0000-000000000003', v_clinic, v_pet_badi, NULL,
     v_vet_marko, 'Nobivac DHPPi + L4', 'C9012-24', 'MSD Animal Health',
     (CURRENT_DATE - INTERVAL '380 days') + TIME '11:00', CURRENT_DATE - INTERVAL '15 days',
     CURRENT_DATE - INTERVAL '2 days', NOW(), NOW(), false, 0),

    ('60000000-0000-0000-0000-000000000004', v_clinic, v_pet_carli, NULL,
     v_vet_marko, 'Nobivac Rabies', 'D3456-24', 'MSD Animal Health',
     (CURRENT_DATE - INTERVAL '360 days') + TIME '14:00', CURRENT_DATE + INTERVAL '6 days',
     CURRENT_DATE + INTERVAL '6 days', NOW(), NOW(), false, 0),

    ('60000000-0000-0000-0000-000000000005', v_clinic, v_pet_lola, NULL,
     v_vet_marko, 'Nobivac Puppy DP', 'E7890-25', 'MSD Animal Health',
     (CURRENT_DATE - INTERVAL '30 days') + TIME '10:00', CURRENT_DATE + INTERVAL '21 days',
     CURRENT_DATE, NOW(), NOW(), false, 0),

    ('60000000-0000-0000-0000-000000000006', v_clinic, v_pet_sima, NULL,
     v_vet_ana, 'Nobivac Tricat Trio', 'F1234-24', 'MSD Animal Health',
     (CURRENT_DATE - INTERVAL '350 days') + TIME '16:00', CURRENT_DATE + INTERVAL '15 days',
     CURRENT_DATE + INTERVAL '4 days', NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 7. INVOICES
-- ============================================================
INSERT INTO invoice (id, clinic_id, owner_id, location_id, appointment_id, invoice_number, status, issued_at, due_date, subtotal, tax_amount, discount_amount, total, currency, note, created_at, updated_at, deleted, version)
VALUES
    ('70000000-0000-0000-0000-000000000001', v_clinic, v_owner_nenad,
     v_location, '40000000-0000-0000-0000-000000000001',
     'FAK-2026-001', 'PAID', (CURRENT_DATE - INTERVAL '14 days')::timestamptz, CURRENT_DATE - INTERVAL '7 days',
     2500.00, 500.00, 0.00, 3000.00, 'RSD', 'Pregled - Nora', NOW(), NOW(), false, 0),

    ('70000000-0000-0000-0000-000000000002', v_clinic, v_owner_ivana,
     v_location, '40000000-0000-0000-0000-000000000002',
     'FAK-2026-002', 'PAID', (CURRENT_DATE - INTERVAL '10 days')::timestamptz, CURRENT_DATE - INTERVAL '3 days',
     3000.00, 600.00, 0.00, 3600.00, 'RSD', 'Vakcinacija - Bella', NOW(), NOW(), false, 0),

    ('70000000-0000-0000-0000-000000000003', v_clinic, v_owner_stefan,
     v_location, '40000000-0000-0000-0000-000000000003',
     'FAK-2026-003', 'PAID', (CURRENT_DATE - INTERVAL '7 days')::timestamptz, CURRENT_DATE,
     8000.00, 1600.00, 0.00, 9600.00, 'RSD', 'Kastracija - Badi', NOW(), NOW(), false, 0),

    ('70000000-0000-0000-0000-000000000004', v_clinic, v_owner_jelena,
     v_location, '40000000-0000-0000-0000-000000000004',
     'FAK-2026-004', 'PAID', (CURRENT_DATE - INTERVAL '5 days')::timestamptz, CURRENT_DATE + INTERVAL '2 days',
     7000.00, 1400.00, 0.00, 8400.00, 'RSD', 'Pregled + lab - Mika', NOW(), NOW(), false, 0),

    ('70000000-0000-0000-0000-000000000005', v_clinic, v_owner_dragan,
     v_location, '40000000-0000-0000-0000-000000000005',
     'FAK-2026-005', 'PAID', (CURRENT_DATE - INTERVAL '3 days')::timestamptz, CURRENT_DATE + INTERVAL '4 days',
     2500.00, 500.00, 0.00, 3000.00, 'RSD', 'Gastritis - Čarli', NOW(), NOW(), false, 0),

    ('70000000-0000-0000-0000-000000000006', v_clinic, v_owner_maja,
     v_location, '40000000-0000-0000-0000-000000000006',
     'FAK-2026-006', 'ISSUED', (CURRENT_DATE - INTERVAL '2 days')::timestamptz, CURRENT_DATE + INTERVAL '12 days',
     9000.00, 1800.00, 0.00, 10800.00, 'RSD', 'Stomatologija - Roksi', NOW(), NOW(), false, 0),

    ('70000000-0000-0000-0000-000000000007', v_clinic, v_owner_nenad,
     v_location, NULL,
     'FAK-2026-007', 'DRAFT', NULL, NULL,
     2500.00, 500.00, 0.00, 3000.00, 'RSD', 'Vakcinacija - Rio', NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 8. INVOICE ITEMS (service_id found dynamically)
-- ============================================================
INSERT INTO invoice_item (id, clinic_id, invoice_id, service_id, description, quantity, unit_price, tax_rate, discount_percent, line_total, sort_order, created_at, updated_at, deleted, version)
VALUES
    ('80000000-0000-0000-0000-000000000001', v_clinic, '70000000-0000-0000-0000-000000000001',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND name ILIKE '%opšti pregled%' AND deleted = false LIMIT 1),
     'Opšti pregled', 1, 2500.00, 20.00, 0, 3000.00, 1, NOW(), NOW(), false, 0),

    ('80000000-0000-0000-0000-000000000003', v_clinic, '70000000-0000-0000-0000-000000000002',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND category = 'VACCINATION' AND deleted = false LIMIT 1),
     'Vakcinacija mačaka', 1, 3000.00, 20.00, 0, 3600.00, 1, NOW(), NOW(), false, 0),

    ('80000000-0000-0000-0000-000000000004', v_clinic, '70000000-0000-0000-0000-000000000003',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND category = 'SURGERY' AND deleted = false LIMIT 1),
     'Kastracija pas', 1, 8000.00, 20.00, 0, 9600.00, 1, NOW(), NOW(), false, 0),

    ('80000000-0000-0000-0000-000000000005', v_clinic, '70000000-0000-0000-0000-000000000004',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND name ILIKE '%opšti pregled%' AND deleted = false LIMIT 1),
     'Opšti pregled', 1, 2500.00, 20.00, 0, 3000.00, 1, NOW(), NOW(), false, 0),
    ('80000000-0000-0000-0000-000000000006', v_clinic, '70000000-0000-0000-0000-000000000004',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND category = 'LAB' AND deleted = false LIMIT 1),
     'Kompletna krvna slika', 1, 3000.00, 20.00, 0, 3600.00, 2, NOW(), NOW(), false, 0),
    ('80000000-0000-0000-0000-000000000007', v_clinic, '70000000-0000-0000-0000-000000000004',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND category = 'LAB' AND deleted = false OFFSET 1 LIMIT 1),
     'Test na parazite', 1, 1500.00, 20.00, 0, 1800.00, 3, NOW(), NOW(), false, 0),

    ('80000000-0000-0000-0000-000000000008', v_clinic, '70000000-0000-0000-0000-000000000005',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND name ILIKE '%opšti pregled%' AND deleted = false LIMIT 1),
     'Opšti pregled', 1, 2500.00, 20.00, 0, 3000.00, 1, NOW(), NOW(), false, 0),

    ('80000000-0000-0000-0000-000000000009', v_clinic, '70000000-0000-0000-0000-000000000006',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND category = 'DENTAL' AND deleted = false LIMIT 1),
     'Čišćenje zubnog kamenca', 1, 6000.00, 20.00, 0, 7200.00, 1, NOW(), NOW(), false, 0),
    ('80000000-0000-0000-0000-000000000010', v_clinic, '70000000-0000-0000-0000-000000000006',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND category = 'DENTAL' AND deleted = false OFFSET 1 LIMIT 1),
     'Vađenje zuba', 1, 3000.00, 20.00, 0, 3600.00, 2, NOW(), NOW(), false, 0),

    ('80000000-0000-0000-0000-000000000011', v_clinic, '70000000-0000-0000-0000-000000000007',
     (SELECT id FROM service WHERE clinic_id = v_clinic AND category = 'VACCINATION' AND deleted = false LIMIT 1),
     'Vakcinacija - besnilo', 1, 2500.00, 20.00, 0, 3000.00, 1, NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 9. INVENTORY ITEMS
-- ============================================================
INSERT INTO inventory_item (id, clinic_id, location_id, name, sku, category, quantity_on_hand, unit, reorder_level, cost_price, sell_price, expiry_date, active, created_at, updated_at, deleted, version)
VALUES
    ('90000000-0000-0000-0000-000000000001', v_clinic, v_location, 'Nobivac Rabies', 'VAK-001', 'MEDICATION', 25, 'doza', 10, 800.00, 2500.00, CURRENT_DATE + INTERVAL '8 months', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000002', v_clinic, v_location, 'Nobivac DHPPi + L4', 'VAK-002', 'MEDICATION', 15, 'doza', 10, 1200.00, 3500.00, CURRENT_DATE + INTERVAL '6 months', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000003', v_clinic, v_location, 'Nobivac Tricat Trio', 'VAK-003', 'MEDICATION', 12, 'doza', 8, 1000.00, 3000.00, CURRENT_DATE + INTERVAL '10 months', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000004', v_clinic, v_location, 'Amoksicilin 250mg', 'LEK-001', 'MEDICATION', 50, 'tableta', 20, 30.00, 80.00, CURRENT_DATE + INTERVAL '1 year', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000005', v_clinic, v_location, 'Meloksikam 1.5mg/ml', 'LEK-002', 'MEDICATION', 8, 'bočica', 5, 450.00, 1200.00, CURRENT_DATE + INTERVAL '14 months', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000006', v_clinic, v_location, 'Zoletil 50', 'ANE-001', 'MEDICATION', 3, 'bočica', 2, 3500.00, NULL, CURRENT_DATE + INTERVAL '5 months', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000007', v_clinic, v_location, 'NaCl 0.9% - 500ml', 'INF-001', 'MEDICATION', 30, 'boca', 15, 150.00, 500.00, CURRENT_DATE + INTERVAL '2 years', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000008', v_clinic, v_location, 'Hirurške rukavice (M)', 'POT-001', 'SUPPLY', 200, 'par', 50, 15.00, NULL, CURRENT_DATE + INTERVAL '3 years', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000009', v_clinic, v_location, 'Špric 5ml', 'POT-002', 'SUPPLY', 150, 'kom', 50, 10.00, NULL, CURRENT_DATE + INTERVAL '4 years', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000010', v_clinic, v_location, 'Gaza sterilna 10x10', 'POT-003', 'SUPPLY', 80, 'pak', 30, 50.00, NULL, CURRENT_DATE + INTERVAL '2 years', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000011', v_clinic, v_location, 'Flaster hipoalergijski', 'POT-004', 'SUPPLY', 5, 'rolna', 10, 200.00, NULL, CURRENT_DATE + INTERVAL '3 years', true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000012', v_clinic, v_location, 'Elizabetanski okovratnik (M)', 'OPR-001', 'EQUIPMENT', 10, 'kom', 5, 300.00, 800.00, NULL, true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000013', v_clinic, v_location, 'Elizabetanski okovratnik (L)', 'OPR-002', 'EQUIPMENT', 6, 'kom', 3, 400.00, 1000.00, NULL, true, NOW(), NOW(), false, 0),
    ('90000000-0000-0000-0000-000000000014', v_clinic, v_location, 'Mikročip ISO 11784', 'OPR-003', 'EQUIPMENT', 20, 'kom', 10, 800.00, 3000.00, NULL, true, NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 10. INVENTORY TRANSACTIONS
-- ============================================================
INSERT INTO inventory_transaction (id, clinic_id, inventory_item_id, type, quantity, reference_type, performed_by, note, created_at, updated_at, deleted, version)
VALUES
    ('a0000001-0000-0000-0000-000000000001', v_clinic, '90000000-0000-0000-0000-000000000001', 'IN', 30, 'PURCHASE', v_admin, 'Nabavka vakcina', NOW() - INTERVAL '30 days', NOW(), false, 0),
    ('a0000001-0000-0000-0000-000000000002', v_clinic, '90000000-0000-0000-0000-000000000002', 'IN', 20, 'PURCHASE', v_admin, 'Nabavka vakcina', NOW() - INTERVAL '30 days', NOW(), false, 0),
    ('a0000001-0000-0000-0000-000000000003', v_clinic, '90000000-0000-0000-0000-000000000003', 'IN', 15, 'PURCHASE', v_admin, 'Nabavka vakcina', NOW() - INTERVAL '30 days', NOW(), false, 0),
    ('a0000001-0000-0000-0000-000000000004', v_clinic, '90000000-0000-0000-0000-000000000004', 'IN', 60, 'PURCHASE', v_admin, 'Nabavka lekova', NOW() - INTERVAL '30 days', NOW(), false, 0),
    ('a0000001-0000-0000-0000-000000000010', v_clinic, '90000000-0000-0000-0000-000000000001', 'OUT', 5, 'TREATMENT', v_vet_marko, 'Vakcinacija', NOW() - INTERVAL '10 days', NOW(), false, 0),
    ('a0000001-0000-0000-0000-000000000011', v_clinic, '90000000-0000-0000-0000-000000000002', 'OUT', 5, 'TREATMENT', v_vet_marko, 'Vakcinacija', NOW() - INTERVAL '10 days', NOW(), false, 0),
    ('a0000001-0000-0000-0000-000000000012', v_clinic, '90000000-0000-0000-0000-000000000003', 'OUT', 3, 'TREATMENT', v_vet_ana, 'Vakcinacija mačaka', NOW() - INTERVAL '8 days', NOW(), false, 0),
    ('a0000001-0000-0000-0000-000000000013', v_clinic, '90000000-0000-0000-0000-000000000004', 'OUT', 10, 'TREATMENT', v_vet_nikola, 'Terapija', NOW() - INTERVAL '5 days', NOW(), false, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 11. PRESCRIPTIONS
-- ============================================================
INSERT INTO prescription (id, clinic_id, medical_record_id, pet_id, vet_id, medication_name, dosage, frequency, duration_days, start_date, end_date, instructions, created_at, updated_at, deleted, version)
VALUES
    (gen_random_uuid(), v_clinic, '50000000-0000-0000-0000-000000000003',
     v_pet_badi, v_vet_marko,
     'Amoksicilin 250mg', '1 tableta', '2x dnevno', 7, CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE,
     'Davati sa hranom.', NOW(), NOW(), false, 0),

    (gen_random_uuid(), v_clinic, '50000000-0000-0000-0000-000000000003',
     v_pet_badi, v_vet_marko,
     'Meloksikam 1.5mg/ml', '0.1 mg/kg', '1x dnevno', 5, CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE - INTERVAL '2 days',
     'Davati ujutru sa hranom.', NOW(), NOW(), false, 0),

    (gen_random_uuid(), v_clinic, '50000000-0000-0000-0000-000000000004',
     v_pet_mika, v_vet_ana,
     'Apoquel 16mg', '1 tableta', '2x dnevno prvih 14 dana, zatim 1x', 30, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '25 days',
     'Kontrola za 14 dana.', NOW(), NOW(), false, 0),

    (gen_random_uuid(), v_clinic, '50000000-0000-0000-0000-000000000005',
     v_pet_carli, v_vet_marko,
     'Omeprazol 20mg', '1 kapsula', '1x dnevno pre jela', 10, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '7 days',
     'Dijeta: pirinač + kuvana piletina.', NOW(), NOW(), false, 0),

    (gen_random_uuid(), v_clinic, '50000000-0000-0000-0000-000000000006',
     v_pet_roksi, v_vet_nikola,
     'Amoksicilin + Klavulanska kis. 500mg', '1 tableta', '2x dnevno', 7, CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE + INTERVAL '5 days',
     'Davati sa hranom. Kontrola za 10 dana.', NOW(), NOW(), false, 0)
ON CONFLICT DO NOTHING;

RAISE NOTICE 'Demo seed data inserted successfully!';
RAISE NOTICE 'Added: 3 vets, 8 owners, 11 pets, 14 appointments,';
RAISE NOTICE '6 medical records, 6 vaccinations, 7 invoices,';
RAISE NOTICE '14 inventory items, 8 transactions, 5 prescriptions';

END $$;

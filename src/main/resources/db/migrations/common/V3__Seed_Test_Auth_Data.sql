-- Test auth data matching Python's make_test_users() and make_applications()
-- Only applied in dev/test environments via Flyway

-- ═══════════════════════════════════════════════════════════════════════════════
-- Django Groups (from mtp_auth/fixtures/initial_groups.json)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO auth_group (id, name) VALUES
  (1, 'PrisonerLocationAdmin'),
  (2, 'BankAdmin'),
  (3, 'PrisonClerk'),
  (4, 'RefundBankAdmin'),
  (5, 'SendMoney'),
  (6, 'UserAdmin'),
  (7, 'Security'),
  (8, 'DisbursementBankAdmin'),
  (9, 'FIU')
ON CONFLICT (name) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- OAuth2 Applications (from Python constants.py and core/tests/utils.py)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO oauth2_provider_application (id, client_id, client_secret, client_type, authorization_grant_type, name)
VALUES
  (1, 'cashbook', 'cashbook', 'confidential', 'password', 'Digital cashbook'),
  (2, 'noms-ops', 'noms-ops', 'confidential', 'password', 'Prisoner money intelligence'),
  (3, 'bank-admin', 'bank-admin', 'confidential', 'password', 'Bank admin'),
  (4, 'send-money', 'send-money', 'confidential', 'password', 'Send money to someone in prison')
ON CONFLICT (client_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Test Users (password = 'testtest' hashed with Django PBKDF2)
-- The hash below is: pbkdf2_sha256$260000$salt$hash for 'testtest'
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO auth_user (id, username, password, email, first_name, last_name, is_staff, is_superuser, is_active, date_joined)
VALUES
  -- Superadmin (all access)
  (1, 'admin', 'pbkdf2_sha256$260000$test$KIk3IPVDsMxIMHBREzJpQAAy+t2kVcBz/sMZPfYtWnM=',
   'admin@mtp.local', 'Admin', 'User', true, true, true, NOW()),

  -- Prison clerk (Cashbook app)
  (2, 'test-prison-clerk-1', 'pbkdf2_sha256$260000$test$KIk3IPVDsMxIMHBREzJpQAAy+t2kVcBz/sMZPfYtWnM=',
   'clerk1@mtp.local', 'Clerk', 'One', false, false, true, NOW()),

  -- Security staff (NomsOps app)
  (3, 'test-security', 'pbkdf2_sha256$260000$test$KIk3IPVDsMxIMHBREzJpQAAy+t2kVcBz/sMZPfYtWnM=',
   'security@mtp.local', 'Security', 'Staff', false, false, true, NOW()),

  -- FIU user (NomsOps app)
  (4, 'test-security-fiu', 'pbkdf2_sha256$260000$test$KIk3IPVDsMxIMHBREzJpQAAy+t2kVcBz/sMZPfYtWnM=',
   'fiu@mtp.local', 'FIU', 'User', false, false, true, NOW()),

  -- Bank admin (BankAdmin app)
  (5, 'test-bank-admin', 'pbkdf2_sha256$260000$test$KIk3IPVDsMxIMHBREzJpQAAy+t2kVcBz/sMZPfYtWnM=',
   'bankadmin@mtp.local', 'Bank', 'Admin', false, false, true, NOW()),

  -- Disbursement bank admin (BankAdmin app)
  (6, 'test-disbursement-admin', 'pbkdf2_sha256$260000$test$KIk3IPVDsMxIMHBREzJpQAAy+t2kVcBz/sMZPfYtWnM=',
   'disbadmin@mtp.local', 'Disbursement', 'Admin', false, false, true, NOW()),

  -- Send money user (SendMoney app)
  (7, 'test-send-money', 'pbkdf2_sha256$260000$test$KIk3IPVDsMxIMHBREzJpQAAy+t2kVcBz/sMZPfYtWnM=',
   'sendmoney@mtp.local', 'Send', 'Money', false, false, true, NOW()),

  -- Prisoner location admin (NomsOps app)
  (8, 'test-prisoner-location-admin', 'pbkdf2_sha256$260000$test$KIk3IPVDsMxIMHBREzJpQAAy+t2kVcBz/sMZPfYtWnM=',
   'locadmin@mtp.local', 'Location', 'Admin', false, false, true, NOW()),

  -- Basic user with NO groups (for testing authenticated but no roles)
  (9, 'test-no-roles', 'pbkdf2_sha256$260000$test$KIk3IPVDsMxIMHBREzJpQAAy+t2kVcBz/sMZPfYtWnM=',
   'noroles@mtp.local', 'No', 'Roles', false, false, true, NOW())
ON CONFLICT (username) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- User → Group Mappings
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO auth_user_groups (user_id, group_id) VALUES
  -- admin gets all groups
  (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
  -- prison clerk → PrisonClerk
  (2, 3),
  -- security → Security
  (3, 7),
  -- FIU → Security + FIU + UserAdmin
  (4, 7), (4, 9), (4, 6),
  -- bank admin → BankAdmin + RefundBankAdmin
  (5, 2), (5, 4),
  -- disbursement admin → DisbursementBankAdmin
  (6, 8),
  -- send money → SendMoney
  (7, 5),
  -- prisoner location admin → PrisonerLocationAdmin
  (8, 1)
ON CONFLICT (user_id, group_id) DO NOTHING;

-- ═════════════════════════════════════════════════════════════════��═════════════
-- User → Application Mappings
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO mtp_auth_applicationusermapping (user_id, application_id) VALUES
  -- admin can access all apps
  (1, 1), (1, 2), (1, 3), (1, 4),
  -- prison clerk → cashbook
  (2, 1),
  -- security → noms-ops
  (3, 2),
  -- fiu → noms-ops
  (4, 2),
  -- bank admin → bank-admin
  (5, 3),
  -- disbursement admin → bank-admin
  (6, 3),
  -- send money → send-money
  (7, 4),
  -- prisoner location admin → noms-ops
  (8, 2)
ON CONFLICT (user_id, application_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Pre-created Access Tokens (for test suites)
-- Expire far in the future so tests don't need to refresh
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO oauth2_provider_accesstoken (token, expires, scope, user_id, application_id) VALUES
  -- Admin token (all permissions via all groups)
  ('test-token-admin', '2030-12-31 23:59:59+00', 'read write', 1, 1),
  -- Prison clerk token (Cashbook app)
  ('test-token-prison-clerk', '2030-12-31 23:59:59+00', 'read write', 2, 1),
  -- Security staff token (NomsOps app)
  ('test-token-security', '2030-12-31 23:59:59+00', 'read write', 3, 2),
  -- FIU token (NomsOps app)
  ('test-token-fiu', '2030-12-31 23:59:59+00', 'read write', 4, 2),
  -- Bank admin token (BankAdmin app)
  ('test-token-bank-admin', '2030-12-31 23:59:59+00', 'read write', 5, 3),
  -- Disbursement admin token (BankAdmin app)
  ('test-token-disbursement-admin', '2030-12-31 23:59:59+00', 'read write', 6, 3),
  -- Send money token (SendMoney app)
  ('test-token-send-money', '2030-12-31 23:59:59+00', 'read write', 7, 4),
  -- Prisoner location admin token (NomsOps app)
  ('test-token-prisoner-location-admin', '2030-12-31 23:59:59+00', 'read write', 8, 2),
  -- No-roles token (authenticated but no groups/permissions)
  ('test-token-no-roles', '2030-12-31 23:59:59+00', 'read write', 9, 1)
ON CONFLICT (token) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Reference Data: Prisons (matching Python test_prisons.json)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO prison_prison (nomis_id, name, region, pre_approval_required, private_estate, use_nomis_for_balances, created, modified)
VALUES
  ('IXB', 'Prison 1', 'West Midlands', false, false, false, NOW(), NOW()),
  ('INP', 'Prison 2', 'London', false, false, false, NOW(), NOW()),
  ('LEI', 'Leeds', 'Yorkshire', false, false, false, NOW(), NOW())
ON CONFLICT (nomis_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Reference Data: Prison Categories (matching Python initial_types.json)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO prison_category (category_id, name) VALUES
  (1, 'Category A'),
  (2, 'Category B'),
  (3, 'Category C'),
  (4, 'Category D'),
  (5, 'Young Offender Institution (YOI)'),
  (6, 'Immigration Removal Centre (IRC)'),
  (7, 'Foreign National Prison (FNP)')
ON CONFLICT (category_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Reference Data: Prison Populations (matching Python initial_types.json)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO prison_population (population_id, name) VALUES
  (1, 'male'),
  (2, 'female'),
  (3, 'adult'),
  (4, 'young')
ON CONFLICT (population_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Prisoner Locations (for disbursement validation)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO prison_prisonerlocation (prisoner_number, prison_id, active, created_by, created, modified)
VALUES
  ('A1409AE', 'IXB', true, 'test-seed', NOW(), NOW()),
  ('A1617FY', 'INP', true, 'test-seed', NOW(), NOW())
ON CONFLICT DO NOTHING;

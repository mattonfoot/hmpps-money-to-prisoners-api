-- Test-only seed data: replicates the minimal user/group/application/token graph
-- that Django's `make_test_users` + `make_applications` produce, so the Kotlin
-- DjangoOAuth2AuthenticationFilter can authenticate the test-token-* opaque
-- tokens used in IntegrationTestBase.kt.

-- 1) Groups (matches mtp_auth/fixtures/initial_groups.json)
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
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('auth_group', 'id'), 100, false);

-- 2) OAuth2 applications (matches make_applications)
-- Django's password = pbkdf2_sha256$... but we can stash a plain marker since
-- we never authenticate via password — only opaque tokens.
INSERT INTO auth_user
  (id, password, last_login, is_superuser, username, first_name, last_name,
   email, is_staff, is_active, date_joined)
VALUES
  (1, '!unusable', NULL, true,  'admin',                         '', '', 'admin@mtp.local',          true,  true, NOW()),
  (2, '!unusable', NULL, false, 'prison-clerk',                  '', '', 'clerk@mtp.local',          false, true, NOW()),
  (3, '!unusable', NULL, false, 'prison-clerk-ua',               '', '', 'clerk-ua@mtp.local',       false, true, NOW()),
  (4, '!unusable', NULL, false, 'prison-security',               '', '', 'prison-security@mtp.local',false, true, NOW()),
  (5, '!unusable', NULL, false, 'pla-user-admin',                '', '', 'pla-ua@mtp.local',         false, true, NOW()),
  (6, '!unusable', NULL, false, 'prisoner-location-admin',       '', '', 'pla@mtp.local',            false, true, NOW()),
  (7, '!unusable', NULL, false, 'security-fiu-0',                '', '', 'fiu@mtp.local',            false, true, NOW()),
  (8, '!unusable', NULL, false, 'security-staff',                '', '', 'security@mtp.local',       false, true, NOW()),
  (9, '!unusable', NULL, false, 'refund-bank-admin',             '', '', 'rba@mtp.local',            false, true, NOW()),
  (10,'!unusable', NULL, false, 'bank-admin',                    '', '', 'ba@mtp.local',             false, true, NOW()),
  (11,'!unusable', NULL, false, 'rba-user-admin-1',              '', '', 'rba-ua-1@mtp.local',       false, true, NOW()),
  (12,'!unusable', NULL, false, 'disbursement-bank-admin',       '', '', 'dba@mtp.local',            false, true, NOW()),
  (13,'!unusable', NULL, false, 'send-money',                    '', '', 'sm@mtp.local',             false, true, NOW()),
  (14,'!unusable', NULL, false, 'no-roles',                      '', '', 'noroles@mtp.local',        false, true, NOW())
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('auth_user', 'id'), 100, false);

-- 3) Group memberships
INSERT INTO auth_user_groups (user_id, group_id) VALUES
  -- test-admin (superuser) gets all groups so any @PreAuthorize check passes
  (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
  -- test-prison-clerk → PrisonClerk
  (2, 3),
  -- test-prison-clerk-ua → PrisonClerk + UserAdmin
  (3, 3), (3, 6),
  -- test-prison-security → Security (prison-scoped)
  (4, 7),
  -- test-pla-user-admin → PrisonerLocationAdmin + UserAdmin
  (5, 1), (5, 6),
  -- test-prisoner-location-admin → PrisonerLocationAdmin
  (6, 1),
  -- security-fiu-0 → Security + FIU
  (7, 7), (7, 9),
  -- test-security → Security
  (8, 7),
  -- test-refund-bank-admin → RefundBankAdmin
  (9, 4),
  -- test-bank-admin → BankAdmin + RefundBankAdmin
  (10, 2), (10, 4),
  -- test-rba-user-admin-1 → RefundBankAdmin + UserAdmin
  (11, 4), (11, 6),
  -- test-disbursement-admin → DisbursementBankAdmin
  (12, 8),
  -- test-send-money → SendMoney
  (13, 5)
ON CONFLICT (user_id, group_id) DO NOTHING;

-- 4) OAuth2 applications
INSERT INTO oauth2_provider_application
  (id, client_id, redirect_uris, client_type, authorization_grant_type,
   client_secret, name, user_id, skip_authorization, created, updated,
   algorithm, post_logout_redirect_uris, hash_client_secret, allowed_origins)
VALUES
  (1, 'cashbook',  '', 'confidential', 'password', 'cashbook',  'Digital cashbook',                1, false, NOW(), NOW(), '', '', false, ''),
  (2, 'noms-ops',  '', 'confidential', 'password', 'noms-ops',  'Prisoner money intelligence',     1, false, NOW(), NOW(), '', '', false, ''),
  (3, 'bank-admin','', 'confidential', 'password', 'bank-admin','Bank admin',                       1, false, NOW(), NOW(), '', '', false, ''),
  (4, 'send-money','', 'confidential', 'password', 'send-money','Send money to someone in prison',  1, false, NOW(), NOW(), '', '', false, '')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('oauth2_provider_application', 'id'), 100, false);

-- 5) Pre-shared opaque access tokens
-- token_checksum = sha256(token) hex per django-oauth-toolkit
INSERT INTO oauth2_provider_accesstoken
  (token, token_checksum, expires, scope, application_id, user_id, created, updated)
VALUES
  ('test-token-admin',                    encode(sha256('test-token-admin'::bytea),                    'hex'), '2099-12-31 23:59:59+00', 'read write', 1, 1,  NOW(), NOW()),
  ('test-token-bank-admin',               encode(sha256('test-token-bank-admin'::bytea),               'hex'), '2099-12-31 23:59:59+00', 'read write', 3, 10, NOW(), NOW()),
  ('test-token-security',                 encode(sha256('test-token-security'::bytea),                 'hex'), '2099-12-31 23:59:59+00', 'read write', 2, 8,  NOW(), NOW()),
  ('test-token-fiu',                      encode(sha256('test-token-fiu'::bytea),                      'hex'), '2099-12-31 23:59:59+00', 'read write', 2, 7,  NOW(), NOW()),
  ('test-token-prison-clerk',             encode(sha256('test-token-prison-clerk'::bytea),             'hex'), '2099-12-31 23:59:59+00', 'read write', 1, 2,  NOW(), NOW()),
  ('test-token-prison-clerk-ua',          encode(sha256('test-token-prison-clerk-ua'::bytea),          'hex'), '2099-12-31 23:59:59+00', 'read write', 1, 3,  NOW(), NOW()),
  ('test-token-no-roles',                 encode(sha256('test-token-no-roles'::bytea),                 'hex'), '2099-12-31 23:59:59+00', 'read write', 1, 14, NOW(), NOW()),
  ('test-token-disbursement-admin',       encode(sha256('test-token-disbursement-admin'::bytea),       'hex'), '2099-12-31 23:59:59+00', 'read write', 3, 12, NOW(), NOW()),
  ('test-token-send-money',               encode(sha256('test-token-send-money'::bytea),               'hex'), '2099-12-31 23:59:59+00', 'read write', 4, 13, NOW(), NOW()),
  ('test-token-prisoner-location-admin',  encode(sha256('test-token-prisoner-location-admin'::bytea),  'hex'), '2099-12-31 23:59:59+00', 'read write', 2, 6,  NOW(), NOW())
ON CONFLICT (token_checksum) DO NOTHING;

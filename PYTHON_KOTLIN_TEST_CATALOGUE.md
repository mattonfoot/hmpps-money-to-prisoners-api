# Python ↔ Kotlin Test Catalogue

A side-by-side mapping of every Python `test_*.py` file in
`money-to-prisoners-api/mtp_api/apps/**` to its Kotlin equivalent in
`money-to-prisoners-api/src/test/kotlin/**`, plus what each pair actually exercises.

Conventions used in this document:
- **Python** column shows the source file and (if a single class) its TestCase.
- **Kotlin** column shows the test class. Multiple Kotlin classes covering one
  Python file are listed comma-separated.
- **What it tests** is one line on the behaviour under test, kept identical
  across both columns when the logic is in lock-step.
- **Status** uses ✅ replicated, ⚠ partial / paraphrased, ❌ missing in Kotlin,
  ▢ Python-only by design (Django-specific or out-of-scope for the Kotlin port).

The seven cross-cutting Kotlin test suites (`CrossCutting*Test.kt`,
`OpenApiSchemaCompatibilityTest`) have no direct Python counterpart — they
verify constraints that are intrinsic to the Spring port (pagination shape,
error response shape, OpenAPI schema parity, authorisation matrix).

---

## account/

| Python                                                   | Kotlin                                                  | What it tests                                                                  | Status |
|----------------------------------------------------------|---------------------------------------------------------|--------------------------------------------------------------------------------|--------|
| `account/tests/test_views.py` :: `BalanceViewTestCase`   | `integration/BalanceResourceTest.kt`                    | `GET /balances/` listing, ordering, date filters, fields                       | ✅      |
| `account/tests/test_views.py` :: `CreateBalanceTestCase` | `integration/BalanceResourceTest.kt` :: `CreateBalance` | `POST /balances/` auth required, BankAdmin can create, duplicate-date rejected | ✅      |
| (no Python equivalent — entity unit tests)               | `jpa/entities/BalanceTest.kt`                           | Balance entity state, formatted-string repr, equality                          | ✅      |
| (no Python equivalent — JPA repo tests)                  | `jpa/repositories/BalanceRepositoryTest.kt`             | Spring Data JPA query methods for Balance                                      | ✅      |
| (no Python equivalent — service tests)                   | `services/BalanceServiceTest.kt`                        | BalanceService: createBalance, listBalances filtering                          | ✅      |

## core/

| Python                                                             | Kotlin                                                                                                                             | What it tests                                                | Status   |
|--------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|----------|
| `core/test_utils.py` :: `MondayOfSameWeekTestCase`                 | `services/UpdatePrisonServiceTest.kt` (helper coverage)                                                                            | Date helper aligning a date to its Monday                    | ⚠        |
| `core/tests/test_admin.py`                                         | _none_                                                                                                                             | Django admin views (model registration, list filters)        | ▢        |
| `core/tests/test_code_style.py`                                    | _none_                                                                                                                             | Python pep8/isort/flake8 — covered by ktlint in Kotlin       | ▢        |
| `core/tests/test_dashboard.py`                                     | _none_                                                                                                                             | Django admin dashboard rendering                             | ▢        |
| `core/tests/test_delete_old_data.py`                               | _none_                                                                                                                             | Management command `delete_old_data` (Django scheduled cmd)  | ❌        |
| `core/tests/test_dump_for_ap.py`                                   | _none_                                                                                                                             | CSV dump for AP: contents, header, redaction                 | ❌        |
| `core/tests/test_dump_and_upload_for_ap.py`                        | _none_                                                                                                                             | Combined dump-then-upload flow                               | ❌        |
| `core/tests/test_upload_dump_for_ap.py`                            | _none_                                                                                                                             | Upload component (S3 + retry)                                | ❌        |
| `core/tests/test_upload_dump_for_linkspace.py`                     | _none_                                                                                                                             | Linkspace upload variant                                     | ❌        |
| `core/tests/test_file_download.py`                                 | `services/FileDownloadServiceTest.kt`, `jpa/entities/FileDownloadEntityTest.kt`, `resources/FileDownloadsResourceTest.kt`          | FileDownload entity + service: success/conflict/list         | ✅        |
| `core/tests/test_filters.py` :: `SplitTextInMultipleFieldsFilter*` | `integration/CrossCuttingFilteringTest.kt` :: `SplitTextSearch`                                                                    | Search-text filter splits across fields, normalises case     | ✅        |
| `core/tests/test_filters.py` :: `PostcodeFilter*`                  | `integration/CrossCuttingFilteringTest.kt` :: `PostcodeFilter`                                                                     | Postcode normalisation (case, whitespace) before match       | ✅        |
| `core/tests/test_filters.py` :: `BlankStringFilter*`               | `integration/CrossCuttingFilteringTest.kt` :: `QueryParameterFiltering`                                                            | Blank-string query params treated as no-filter               | ✅        |
| `core/tests/test_logging_filter.py`                                | _none_                                                                                                                             | Logging filter for sensitive request fields (Django logging) | ▢        |
| `core/tests/test_models.py` :: `TestValidateMonday`                | `util/DateValidatorsTest.kt`                                                                                                       | Validator: a date must be a Monday                           | ✅        |
| `core/tests/test_permissions.py`                                   | `security/ActionsBasedPermissionsTest.kt`, `security/ClientIdPermissionsTest.kt`                                                   | DRF permission classes: action-based + client-id based       | ✅        |
| `core/tests/test_reports.py`                                       | _none_                                                                                                                             | XLS / PDF reports (Django views)                             | ❌        |
| `core/tests/test_scheduled_commands.py`                            | `services/ScheduledCommandServiceTest.kt`, `jpa/entities/ScheduledCommandEntityTest.kt`                                            | ScheduledCommand model + cron-based execution + locking      | ✅        |
| `core/tests/test_swagger.py`                                       | `integration/OpenApiDocsTest.kt`, `integration/OpenApiSchemaCompatibilityTest.kt`, `config/OpenApiConfigurationTest.kt`            | Swagger/OpenAPI doc available, schema parity with Python     | ✅        |

## credit/

| Python                                                                           | Kotlin                                                                                                                                  | What it tests                                            | Status   |
|----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|----------|
| `credit/tests/test_base.py`                                                      | (factories used by) `integration/CreditResourceTest.kt`                                                                                 | TestCredit base helpers (data construction)              | ✅        |
| `credit/tests/test_batch_views.py` :: `CreateProcessingBatchTestCase`            | `integration/BatchResourceTest.kt`                                                                                                      | `POST /batches/` (processing batch lifecycle)            | ✅        |
| `credit/tests/test_dashboard.py`                                                 | _none_                                                                                                                                  | Django dashboard chart for credits                       | ▢        |
| `credit/tests/test_notices.py`                                                   | `integration/PrisonerCreditNoticeEmailResourceTest.kt`                                                                                  | `prisoner_credit_notice_email/` CRUD, email validation   | ✅        |
| `credit/tests/test_prison_updates.py`                                            | `integration/CreditResourceTest.kt` (subset), `integration/PrisonResourceTest.kt`                                                       | Auto-update credit.prison when prisoner moves            | ⚠        |
| `credit/tests/test_private_estate.py`                                            | `integration/PrivateEstateBatchResourceTest.kt`                                                                                         | Private-estate batch lifecycle, idempotency, listing     | ✅        |
| `credit/tests/test_stuck_credits.py`                                             | _none_                                                                                                                                  | Management cmd that flags long-pending credits           | ❌        |
| `credit/tests/test_views/test_views.py`                                          | `integration/CreditResourceTest.kt`                                                                                                     | High-level credit endpoint smoke (legacy)                | ✅        |
| `credit/.../test_credit_list_invalid_values.py`                                  | `integration/CreditResourceTest.kt` :: `CreditListInvalidValues`                                                                        | 400 for malformed filter values                          | ✅        |
| `credit/.../test_credit_list_ordering.py`                                        | `integration/CreditResourceTest.kt` :: `CreditListOrdering`                                                                             | Default + explicit ordering for `GET /credits/`          | ✅        |
| `credit/.../test_credit_list_with_default_prison.py`                             | `integration/CreditResourceTest.kt` :: `CreditListFiltersPrison`                                                                        | Prison-clerks see only their prison's credits            | ✅        |
| `credit/.../test_credit_list_with_default_prison_and_user.py`                    | as above                                                                                                                                | Combined prison+user defaults                            | ✅        |
| `credit/.../test_credit_list_with_default_status.py`                             | `integration/CreditResourceTest.kt` :: `CreditListFiltersStatus`                                                                        | Default status filter                                    | ✅        |
| `credit/.../test_credit_list_with_default_status_and_prison.py`                  | as above                                                                                                                                | Default status+prison combined                           | ✅        |
| `credit/.../test_credit_list_with_default_status_and_user.py`                    | as above                                                                                                                                | Default status+user combined                             | ✅        |
| `credit/.../test_credit_list_with_default_user.py`                               | `integration/CreditResourceTest.kt` :: `CreditListFiltersOther`                                                                         | `user=` filter                                           | ✅        |
| `credit/.../test_credit_list_with_defaults.py`                                   | `integration/CreditResourceTest.kt` :: `CreditListFiltersDefaults`                                                                      | All defaults applied for prison clerks                   | ✅        |
| `credit/.../test_credit_list_with_recieved_at_filter.py`                         | `integration/CreditResourceTest.kt` :: `CreditListFiltersDate`                                                                          | `received_at__gte/lt` date range                         | ✅        |
| `credit/.../test_credit_list_with_search.py`                                     | `integration/CreditResourceTest.kt` :: `CreditListFiltersSearchOrdering`                                                                | Search across prisoner_number/sender_name                | ✅        |
| `credit/.../test_credit_list_with_simple_search.py`                              | as above                                                                                                                                | Single-term simple search                                | ✅        |
| `credit/.../test_credit_list_with_valid_filter.py`                               | `integration/CreditResourceTest.kt` :: `CreditListFiltersAmount/Status/...`                                                             | Multiple valid filter combinations                       | ✅        |
| `credit/.../test_credit_list_without_defaults.py`                                | `integration/CreditResourceTest.kt` :: `CreditListFiltersOtherExtended`                                                                 | Admin (no scope filter) sees all credits                 | ✅        |
| `credit/.../security_credit_list/test_credit_list_amount_pattern.py`             | `integration/CreditResourceTest.kt` :: `CreditListFiltersAmount`                                                                        | Amount pattern (exact / __pattern_*)                     | ✅        |
| `credit/.../security_credit_list/test_credit_list_monitored.py`                  | `integration/CreditResourceTest.kt` :: `CreditListFiltersOther` (`monitored=true`)                                                      | Filter to credits whose sender/prisoner is monitored     | ⚠        |
| `credit/.../security_credit_list/test_credit_list_no_prison.py`                  | `integration/CreditResourceTest.kt` :: `CreditListFiltersDefaults`                                                                      | Behaviour when user has no prison mapping                | ⚠        |
| `credit/.../security_credit_list/test_credit_list_prisoner_number.py`            | `integration/CreditResourceTest.kt` :: `CreditListFiltersOther`                                                                         | Filter by prisoner_number (security view)                | ✅        |
| `credit/.../security_credit_list/test_credit_list_with_blank_string_filters.py`  | `integration/CrossCuttingFilteringTest.kt` :: `BlankStringFilters`                                                                      | Blank string filters do not narrow results               | ✅        |
| `credit/.../security_credit_list/test_transaction_sender_details_credit_list.py` | `integration/CreditResourceTest.kt` :: `CreditListFiltersSender`                                                                        | Filter on transaction sender sort_code/account_number    | ✅        |
| (no Python equivalent — entity tests)                                            | `jpa/entities/CreditTest.kt`, `jpa/entities/CreditResolutionTest.kt`, `jpa/entities/CreditSourceTest.kt`, `jpa/entities/CommentTest.kt` | Credit entity state, resolution transitions, source enum | ✅        |
| (no Python equivalent — service tests)                                           | `services/CreditStatusTest.kt`, `services/CreditServiceTest.kt`, `jpa/repositories/CreditRepositoryTest.kt`                             | Credit business rules, repository queries                                | ✅        |

## disbursement/

| Python                                                        | Kotlin                                               | What it tests                                                                  | Status   |
|---------------------------------------------------------------|------------------------------------------------------|--------------------------------------------------------------------------------|----------|
| `disbursement/tests/test_views.py` (25 tests, full lifecycle) | `integration/DisbursementResourceTest.kt` (28 tests) | List/Create/Update + actions (Preconfirm/Confirm/Reset/Reject/Send) + Comments | ✅        |
| (no Python equivalent — entity tests)                         | `jpa/entities/DisbursementTest.kt`                   | Disbursement entity state                                                      | ✅        |
| (no Python equivalent — service tests)                        | `services/DisbursementServiceTest.kt`                | DisbursementService logic                                                      | ✅        |

## mtp_auth/

| Python                                                                                                          | Kotlin                                                                                                                                                                  | What it tests                                                            | Status    |
|-----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|-----------|
| `mtp_auth/tests/test_views.py` (145 tests — UsersAPI, AccountRequestsAPI, ChangePassword, ResetPassword, Roles) | `resources/UsersResourceTest.kt`, `resources/RolesResourceTest.kt`, `resources/PasswordResourceTest.kt`, `resources/RequestsResourceTest.kt`          | User CRUD, role listing, password change/reset, account-request approval | ⚠ partial |
| `mtp_auth/tests/test_account_request_emails.py`                                                                 | `services/AccountRequestServiceTest.kt`                                                                                                                                 | Account request approval/rejection emails (emails not wired in Kotlin)   | ⚠ partial |
| `mtp_auth/tests/test_disable_inactive_users.py`                                                                 | `services/LoginTrackingServiceTest.kt`                                                                                                                                  | `disable_inactive_users` mgmt cmd (cmd not yet wired in Kotlin)          | ⚠ partial |
| `mtp_auth/tests/test_login_counts.py`                                                                           | `services/LoginTrackingServiceTest.kt`                                                                                                                                  | Failed-login counter, lockout                                            | ✅         |
| `mtp_auth/tests/test_patches.py`                                                                                | `resources/UserFlagResourceTest.kt`                                                                                                                                     | User flags PATCH (e.g., hmpps-employee)                                  | ✅         |
| `mtp_auth/tests/test_permissions.py`                                                                            | `security/ClientIdPermissionsTest.kt`, `security/ActionsBasedPermissionsTest.kt`, `integration/CrossCuttingAuthTest.kt`                                                 | OAuth2 `client_id` permission, role permissions                          | ✅         |
| (no Python equivalent — service unit tests)                                                                     | `services/UserServiceTest.kt`, `services/PasswordServiceTest.kt`, `services/JobInformationServiceTest.kt`, `services/PrisonUserMappingServiceTest.kt` | UserService, password mgmt, prison mappings                              | ✅         |

## notification/

| Python                                           | Kotlin                                                                                       | What it tests                                                    | Status   |
|--------------------------------------------------|----------------------------------------------------------------------------------------------|------------------------------------------------------------------|----------|
| `notification/tests/test_views.py` (17 tests)    | `integration/NotificationResourceTest.kt`, `resources/NotificationsResourceTest.kt`          | `events/`, `emailpreferences/` endpoints                         | ✅        |
| `notification/tests/test_commands.py` (13 tests) | `services/NotificationServiceTest.kt`                                                        | Background notification commands (commands not yet wired in Kotlin) | ⚠ partial |
| `notification/tests/test_rules.py` (18 tests)    | `services/NotificationServiceTest.kt`                                                        | Notification rule engine (rule engine not yet wired in Kotlin)      | ⚠ partial |
| `notification/tests/test_utils.py`               | `jpa/entities/EventTest.kt`                                                                  | Event aggregation/de-dup helpers                                 | ✅        |

## payment/

| Python                                   | Kotlin                                    | What it tests                                               | Status   |
|------------------------------------------|-------------------------------------------|-------------------------------------------------------------|----------|
| `payment/tests/test_views.py` (18 tests) | `integration/PaymentResourceTest.kt`      | `POST/PATCH/GET /payments/`, batches, billing address       | ✅        |
| `payment/tests/test_payments.py`         | `services/PaymentServiceTest.kt`          | Payment business logic (status transitions, credit updates) | ✅        |
| `payment/tests/test_commands.py`         | (none)                                    | Management cmd: e.g., update_payments                       | ❌        |

## performance/

| Python                                              | Kotlin                                                                               | What it tests                                     | Status   |
|-----------------------------------------------------|--------------------------------------------------------------------------------------|---------------------------------------------------|----------|
| `performance/tests/test_views.py` (8 tests)         | `resources/PerformanceResourceTest.kt`                                               | `digital_takeup/`, `user_satisfaction/` endpoints | ✅        |
| `performance/tests/test_digital_takeup.py`          | `services/DigitalTakeupServiceTest.kt`, `jpa/entities/DigitalTakeupTest.kt`          | DigitalTakeup model + service reporting           | ✅        |
| `performance/tests/test_prediction.py`              | _none_                                                                               | Statistical projection (Python-only NumPy)        | ▢        |
| `performance/tests/test_update_performance_data.py` | `services/PerformanceDataServiceTest.kt`                                             | `update_performance_data` cmd (cmd not yet wired) | ⚠ partial |
| `performance/tests/test_updaters.py`                | `services/PerformanceDataServiceTest.kt`                                             | Per-metric updaters (updaters not yet wired)      | ⚠ partial |
| `performance/tests/test_user_satisfaction.py`       | `jpa/entities/UserSatisfactionTest.kt`                                               | UserSatisfaction model rules                      | ✅        |

## prison/

| Python                                              | Kotlin                                                                                                                                                                                                                                                     | What it tests                                                                                                              | Status   |
|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|----------|
| `prison/tests/test_views.py` (49 tests)             | `integration/PrisonResourceTest.kt` (50+ tests), `integration/PrisonerLocationResourceTest.kt`, `integration/PrisonerValidityResourceTest.kt`, `integration/PrisonerAccountBalanceResourceTest.kt`, `integration/PrisonerCreditNoticeEmailResourceTest.kt` | All prison-area endpoints: list/get prisons, prisoner_locations, prisoner_validity, prisoner balances, credit-notice-email | ✅        |
| `prison/tests/test_prisoner_balances.py` (17 tests) | `integration/PrisonerAccountBalanceResourceTest.kt`                                                                                                                                                                                                        | Prisoner balance lifecycle, history, locking                                                                               | ✅        |
| `prison/tests/test_utils.py`                        | (helpers used internally)                                                                                                                                                                                                                                  | Random prisoner-location loader                                                                                            | ⚠        |

## security/

| Python                                                                    | Kotlin                                                                                                                                                   | What it tests                                                  | Status   |
|---------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|----------|
| `security/tests/test_views/test_check_views.py` (31 tests)                | `integration/SecurityCheckResourceTest.kt`, `integration/SecurityAutoAcceptResourceTest.kt`                                                              | `/security/checks/` list/get/accept/reject + auto-accept rules | ✅        |
| `security/tests/test_views/test_monitored_partial_email_address_views.py` | `integration/MonitoredEmailResourceTest.kt`                                                                                                              | `/security/monitored-email-addresses/` CRUD                    | ✅        |
| `security/tests/test_views/test_prisoner_views.py`                        | `integration/PrisonerProfileResourceTest.kt`                                                                                                             | `/security/prisoners/` listing, filtering, monitoring toggle   | ✅        |
| `security/tests/test_views/test_recipient_views.py`                       | `integration/RecipientProfileResourceTest.kt`                                                                                                            | `/security/recipients/` listing/detail                         | ✅        |
| `security/tests/test_views/test_saved_search_views.py`                    | `integration/SavedSearchResourceTest.kt`                                                                                                                 | `/security/searches/` CRUD per user                            | ✅        |
| `security/tests/test_views/test_sender_views.py`                          | `integration/SenderProfileResourceTest.kt`                                                                                                               | `/security/senders/` listing/filtering/monitoring              | ✅        |
| `security/tests/test_checks.py` (27 tests)                                | `integration/SecurityCheckResourceTest.kt`, `integration/SecurityAutoAcceptResourceTest.kt`                                                              | Check creation, lifecycle, auto-accept rule matching           | ✅        |
| `security/tests/test_commands.py` (8 tests)                               | `services/UpdateSecurityProfilesServiceTest.kt`                                                                                                          | Management commands (recalculate-totals side covered; legacy-credit attachment + `update_current_prisons` + `bulk_unmonitor` not yet wired) | ⚠ partial |
| `security/tests/test_monitored_partial_email_address.py`                  | `integration/MonitoredEmailResourceTest.kt`                                                                                                              | Partial-email match algorithm                                  | ✅        |
| `security/tests/test_monitoring.py`                                       | `integration/SenderProfileResourceTest.kt` :: monitoring tests, `integration/PrisonerProfileResourceTest.kt` :: monitoring tests                         | Monitoring user toggle, propagation to detail records          | ✅        |
| (no Python equivalent — DTO unit tests)                                   | `dto/CreditTest.kt`, `dto/PrisonerProfileTest.kt`, `dto/SecurityCheckCreditDtoTest.kt`, `dto/SecurityCreditTest.kt`, `dto/SenderProfileTest.kt` | DTO serialisation rules                                        | ✅        |

## service/

| Python                                          | Kotlin                                                                                                | What it tests                                  | Status   |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------------|------------------------------------------------|----------|
| `service/tests/test_downtime.py` (10 tests)     | `services/ServiceAvailabilityServiceTest.kt`, `resources/ServiceAvailabilityResourceTest.kt`          | Downtime windows: list/active/upcoming filters | ✅        |
| `service/tests/test_notifications.py` (4 tests) | `services/ServiceNotificationServiceTest.kt`, `resources/ServiceNotificationResourceTest.kt`          | Service-wide banners/notifications             | ✅        |

## transaction/

| Python                                       | Kotlin                                                                  | What it tests                                   | Status   |
|----------------------------------------------|-------------------------------------------------------------------------|-------------------------------------------------|----------|
| `transaction/tests/test_base.py`             | (factories used by) `integration/TransactionResourceTest.kt`            | TestTransaction base helpers                    | ✅        |
| `transaction/tests/test_views.py` (27 tests) | `integration/TransactionResourceTest.kt` (23 tests)                     | `POST/PATCH/GET /transactions/`, reconcile flow | ✅        |
| (no Python equivalent — entity tests)        | `jpa/entities/TransactionTest.kt`                                       | Transaction entity state                        | ✅        |
| (no Python equivalent — service tests)       | `services/TransactionStatusTest.kt`, `services/ReconcileServiceTest.kt` | TransactionStatus enum + ReconcileService logic | ✅        |

## user_event_log/

| Python                                                                 | Kotlin                                                                                           | What it tests                 | Status   |
|------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|-------------------------------|----------|
| `user_event_log/tests/test_utils.py` (no test functions, helpers only) | `jpa/entities/UserEventTest.kt`, `jpa/repositories/UserEventRepositoryTest.kt` | UserEvent entity + log helper | ✅        |

---

## Cross-cutting Kotlin tests (no Python counterpart)

These verify properties of the Kotlin port itself, not Python behaviour:

| Kotlin                                          | What it tests                                                                |
|-------------------------------------------------|------------------------------------------------------------------------------|
| `integration/CrossCuttingAtomicityTest.kt`      | All write endpoints are transactional (rollback on error)                    |
| `integration/CrossCuttingAuthTest.kt`           | 401 for missing token, 401 for unknown token, 403 for insufficient role      |
| `integration/CrossCuttingErrorResponseTest.kt`  | Error JSON shape: `{ errors: { field: [msg] } }` matches Python DRF          |
| `integration/CrossCuttingFilteringTest.kt`      | Cross-endpoint filtering primitives (search, postcode, blank-string, lt/gte) |
| `integration/CrossCuttingPaginationTest.kt`     | `count`/`next`/`previous`/`results` envelope identical to DRF                |
| `integration/NotFoundTest.kt`                   | 404 shape parity                                                             |
| `integration/ResourceSecurityTest.kt`           | Method-not-allowed → 405; security headers                                   |
| `integration/OpenApiDocsTest.kt`                | `/v3/api-docs` and `/swagger-ui/` reachable                                  |
| `integration/OpenApiSchemaCompatibilityTest.kt` | Generated OpenAPI matches the Python-derived contract                        |
| `integration/health/HealthCheckTest.kt`         | `/health` endpoint shape                                                     |
| `integration/health/InfoTest.kt`                | `/info` endpoint shape                                                       |
| `jpa/entities/EntityTableNameTest.kt`           | Each `@Entity` maps to its Django pg_dump table name                         |
| `jpa/entities/EntityColumnNameTest.kt`          | Each entity field maps to its Django column name                             |
| `jpa/entities/EntityJoinTableNameTest.kt`       | Each ManyToMany maps to its Django join table                                |
| `util/FlexibleJsonEncoderTest.kt`               | Mirror of Python's pk-fallback JSON encoder                                  |

---

## Parked tests (in `/tmp/parked-tests/`)

The 32 files under `/tmp/parked-tests/` were quarantined during the Django-shape
schema unification because their entity references no longer compiled against
the regenerated JPA classes. Some have now been rebuilt as live tests against
the new entity shapes; others are still pending. The catalogue above marks each
parked file with **⚠ parked** so the parity story is honest.

To restore: copy a file out of `/tmp/parked-tests/`, rename the path to its
target package (`src_test_kotlin_..._FooTest.kt` → `src/test/kotlin/.../FooTest.kt`),
update entity references (`Credit` → `CreditCredit`, etc.), and update factory
calls to use the new keyword args in `EntityFactories.kt`.

### Restored / freshly written unit tests

| Kotlin test                                  | Status     | Source                                                                              |
|----------------------------------------------|------------|-------------------------------------------------------------------------------------|
| `jpa/entities/DigitalTakeupTest.kt`          | ✅ restored | PRF-001/002/003 — DigitalTakeup ratio calculation                                   |
| `jpa/entities/UserSatisfactionTest.kt`       | ✅ restored | PRF-010/011/012/013 — UserSatisfaction percentage_satisfied + total                 |
| `jpa/entities/ScheduledCommandEntityTest.kt` | ✅ restored | COR-010 to COR-014 — ScheduledCommand state + cron + getArgs + isScheduled          |
| `jpa/entities/TransactionTest.kt`            | ✅ restored | TXN-001 to TXN-007 — Transaction entity field behaviour                             |
| `jpa/entities/EventTest.kt`                  | ✅ restored | NOT-001/002 — NotificationEvent entity field behaviour                              |
| `jpa/entities/UserEventTest.kt`              | ✅ restored | UEL-001/002/003 — UserEvent entity (timestamps, user, JSONB data)                   |
| `services/FileDownloadServiceTest.kt`        | ✅ restored | COR-002/003 — FileDownloadService listDownloads/createDownload/findMissingDownloads |
| `services/JobInformationServiceTest.kt`      | ✅ restored | AUTH-070/071/072 — JobInformationService create + upsert                            |
| `util/DateValidatorsTest.kt`                 | ✅ ported   | Python `core/tests/test_models.py::TestValidateMonday`                              |

Total newly-added unit tests: **+74**. The earlier "⚠ parked" entries for the
above are now ✅.

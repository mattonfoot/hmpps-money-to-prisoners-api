package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.ContainersConfig
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration.helpers.IntegrationTestHelpers
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FactoryHooks
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Import(IntegrationTestHelpers::class, ContainersConfig::class)
abstract class IntegrationTestBase {

  @LocalServerPort
  private var port: Int = 0

  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var integrationTestHelpers: IntegrationTestHelpers

  @Autowired
  protected lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var baseAuthUserRepository: AuthUserRepository

  @Autowired
  private lateinit var basePrisonRepository: PrisonRepository

  @BeforeEach
  fun initClients() {
    webTestClient = WebTestClient.bindToServer()
      .baseUrl("http://localhost:$port")
      .build()
    integrationTestHelpers.setWebClient(webTestClient)
    // Wipe domain tables before every test so per-class @BeforeEach setUp() doesn't
    // need to handle FK ordering. Auth seed data persists.
    wipeDataTables()
    // Re-bind factory hooks each time — Spring re-uses one app context but the
    // hooks are JVM-static, so a parallel test class running between sets could
    // otherwise reset them.
    FactoryHooks.prisonResolver = { nomis -> basePrisonRepository.findById(nomis).orElse(null) }
    FactoryHooks.userResolver = { username -> baseAuthUserRepository.findByUsername(username) }
  }

  /**
   * Wipes all domain tables (every table apart from auth/oauth/migration metadata).
   * Order is irrelevant because TRUNCATE … CASCADE handles FKs in one step.
   * Auth tables (auth_user, auth_group, oauth2_*) and Flyway metadata are
   * preserved so seeded test tokens survive the wipe.
   */
  protected fun wipeDataTables() {
    jdbcTemplate.execute(
      """
      TRUNCATE TABLE
        account_balance,
        core_filedownload, core_scheduledcommand,
        credit_comment, credit_credit, credit_creditingtime,
        credit_log, credit_privateestatebatch, credit_processingbatch,
        credit_processingbatch_credits,
        disbursement_comment, disbursement_disbursement, disbursement_log,
        notification_creditevent, notification_disbursementevent,
        notification_emailnotificationpreferences, notification_event,
        notification_prisonerprofileevent, notification_recipientprofileevent,
        notification_senderprofileevent,
        payment_batch, payment_billingaddress, payment_payment,
        performance_digitaltakeup, performance_performancedata,
        performance_usersatisfaction,
        prison_category, prison_population,
        prison_prison, prison_prison_categories, prison_prison_populations,
        prison_prisonbankaccount, prison_prisonerbalance,
        prison_prisonercreditnoticeemail, prison_prisonerlocation,
        prison_remittanceemail,
        security_bankaccount, security_bankaccount_monitoring_users,
        security_banktransferrecipientdetails, security_banktransfersenderdetails,
        security_cardholdername, security_check,
        security_checkautoacceptrule, security_checkautoacceptrulestate,
        security_debitcardsenderdetails,
        security_debitcardsenderdetails_monitoring_users,
        security_monitoredpartialemailaddress,
        security_prisonerprofile, security_prisonerprofile_monitoring_users,
        security_prisonerprofile_prisons, security_prisonerprofile_recipients,
        security_prisonerprofile_senders,
        security_providedprisonername,
        security_recipientprofile, security_recipientprofile_prisons,
        security_savedsearch, security_searchfilter, security_senderemail,
        security_senderprofile, security_senderprofile_prisons,
        service_downtime, service_notification,
        transaction_transaction,
        user_event_log_userevent,
        mtp_auth_accountrequest, mtp_auth_failedloginattempt,
        mtp_auth_jobinformation, mtp_auth_login,
        mtp_auth_prisonusermapping, mtp_auth_prisonusermapping_prisons
      RESTART IDENTITY CASCADE
      """.trimIndent(),
    )
    seedStandardPrisons()
  }

  /**
   * Seeds the canonical prison rows that test factories reference by NOMIS id
   * (LEI, MDI, BAI, BLI, IXB, WSI, BWI). Mirrors the subset Python's
   * `load_test_data` produces from `test_prisons.json`.
   */
  protected fun seedStandardPrisons() {
    jdbcTemplate.execute(
      """
      INSERT INTO prison_prison
        (created, modified, nomis_id, name, general_ledger_code, region,
         pre_approval_required, cms_establishment_code, private_estate, use_nomis_for_balances)
      VALUES
        (NOW(), NOW(), 'LEI', 'HMP Leeds',         '', 'Yorkshire',  false, '', false, true),
        (NOW(), NOW(), 'MDI', 'HMP Moorland',      '', 'Yorkshire',  false, '', false, true),
        (NOW(), NOW(), 'BAI', 'HMP Belmarsh',      '', 'London',     false, '', false, true),
        (NOW(), NOW(), 'BLI', 'HMP Bristol',       '', 'South West', false, '', false, true),
        (NOW(), NOW(), 'IXB', 'HMP Isle of Wight', '', 'South',      false, '', false, true),
        (NOW(), NOW(), 'WSI', 'HMP Wandsworth',    '', 'London',     false, '', false, true),
        (NOW(), NOW(), 'BWI', 'HMP Berwyn',        '', 'Wales',      false, '', false, true)
      ON CONFLICT (nomis_id) DO NOTHING
      """.trimIndent(),
    )
  }

  /**
   * Returns a header-mutating function that sets the Authorization header
   * with a pre-seeded OAuth2 access token from the database.
   *
   * Token selection is based on the requested roles:
   * - ROLE_BANK_ADMIN → test-token-bank-admin
   * - ROLE_PRISON_CLERK → test-token-prison-clerk
   * - ROLE_SECURITY_STAFF → test-token-security
   * - ROLE_SEND_MONEY → test-token-send-money
   * - ROLE_FIU → test-token-fiu
   * - ROLE_NOMS_OPS → test-token-prisoner-location-admin
   * - No specific roles → test-token-admin (superuser with all groups)
   */
  fun setAuthorisation(
    username: String? = null,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit {
    val token = resolveToken(username, roles)
    return { headers -> headers.setBearerAuth(token) }
  }

  private fun resolveToken(username: String?, roles: List<String>): String {
    // When no roles specified: use admin (all groups) if a username was given,
    // otherwise use no-roles token (for negative auth tests)
    if (roles.isEmpty()) return if (username != null) "test-token-admin" else "test-token-no-roles"

    return when {
      roles.any { it.contains("DISBURSEMENT_BANK_ADMIN", ignoreCase = true) } -> "test-token-disbursement-admin"
      roles.any { it.contains("BANK_ADMIN", ignoreCase = true) } -> "test-token-bank-admin"
      roles.any { it.contains("CASHBOOK", ignoreCase = true) } -> "test-token-prison-clerk"
      roles.any { it.contains("PRISON_CLERK", ignoreCase = true) } -> "test-token-prison-clerk"
      roles.any { it.contains("SECURITY_STAFF", ignoreCase = true) || it.contains("SECURITY", ignoreCase = true) } -> "test-token-security"
      roles.any { it.contains("SEND_MONEY", ignoreCase = true) } -> "test-token-send-money"
      roles.any { it.contains("FIU", ignoreCase = true) } -> "test-token-fiu"
      roles.any { it.contains("NOMS_OPS", ignoreCase = true) } -> "test-token-prisoner-location-admin"
      roles.any { it.contains("USER_ADMIN", ignoreCase = true) } -> "test-token-fiu"
      else -> "test-token-no-roles"
    }
  }
}

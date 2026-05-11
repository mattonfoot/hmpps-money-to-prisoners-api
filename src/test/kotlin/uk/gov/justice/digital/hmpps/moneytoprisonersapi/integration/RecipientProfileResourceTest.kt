package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.RecipientProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.RecipientProfileRepository

class RecipientProfileResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var recipientProfileRepository: RecipientProfileRepository

  @Autowired
  private lateinit var disbursementRepository: DisbursementRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  @BeforeEach
  fun setUp() {
    disbursementRepository.deleteAll()
    recipientProfileRepository.deleteAll()
  }

  private fun createRecipientProfile(
    sortCode: String = "112233",
    accountNumber: String = "12345678",
    rollNumber: String = "",
  ): RecipientProfile {
    val profile = recipientProfileRepository.save(RecipientProfile())
    // Django stores the bank-transfer details on per-detail children; seed
    // one row (bank account + recipient detail) so the DTO surfaces sortCode
    // and accountNumber through the child join.
    val bankAccountId = jdbcTemplate.queryForObject(
      """
      INSERT INTO security_bankaccount (sort_code, account_number, roll_number)
      VALUES (?, ?, ?) RETURNING id
      """.trimIndent(),
      Long::class.java,
      sortCode,
      accountNumber,
      rollNumber,
    )!!
    jdbcTemplate.update(
      """
      INSERT INTO security_banktransferrecipientdetails
        (created, modified, recipient_id, recipient_bank_account_id)
      VALUES (NOW(), NOW(), ?, ?)
      """.trimIndent(),
      profile.id,
      bankAccountId,
    )
    return profile
  }

  /** Variant: profile + detail + monitoring row linking it to [userId]. */
  private fun createRecipientProfileMonitoredBy(
    userId: Long,
    sortCode: String = "112233",
    accountNumber: String = "12345678",
  ): RecipientProfile {
    val profile = createRecipientProfile(sortCode, accountNumber)
    jdbcTemplate.update(
      """
      INSERT INTO security_bankaccount_monitoring_users (bankaccount_id, user_id)
      SELECT r.recipient_bank_account_id, ?
      FROM security_banktransferrecipientdetails r WHERE r.recipient_id = ?
      """.trimIndent(),
      userId,
      profile.id,
    )
    return profile
  }

  @Nested
  @DisplayName("GET /security/recipients/ (RecipientProfileListTestCase)")
  inner class ListRecipientProfiles {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/recipients/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("Returns 403 for user without required role")
    fun `should return 403 without security role`() {
      webTestClient.get()
        .uri("/recipients/")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("Returns paginated list of recipient profiles")
    fun `should return paginated list of recipient profiles`() {
      createRecipientProfile("112233", "12345678")
      createRecipientProfile("445566", "87654321")

      webTestClient.get()
        .uri("/recipients/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results").isArray
    }

    @Test
    @DisplayName("Returns empty list when no recipient profiles")
    fun `should return empty list when no profiles`() {
      webTestClient.get()
        .uri("/recipients/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("Filters by monitoring=true returns only recipients monitored by current user")
    fun `should filter by monitoring true`() {
      val monitoredProfile = createRecipientProfileMonitoredBy(userId = 8L, sortCode = "112233", accountNumber = "11111111")
      createRecipientProfile("445566", "22222222")

      webTestClient.get()
        .uri("/recipients/?monitoring=true")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].id").isEqualTo(monitoredProfile.id!!.toInt())
    }

    @Test
    @DisplayName("Filters by monitoring=false returns recipients NOT monitored by current user")
    fun `should filter by monitoring false`() {
      createRecipientProfileMonitoredBy(userId = 8L, sortCode = "112233", accountNumber = "11111111")
      createRecipientProfile("445566", "22222222")
      createRecipientProfile("778899", "33333333")

      webTestClient.get()
        .uri("/recipients/?monitoring=false")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("GET /security/recipients/{id}/ (RecipientProfileListTestCase - detail)")
  inner class GetRecipientProfile {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      val profile = createRecipientProfile()
      webTestClient.get()
        .uri("/recipients/${profile.id}/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("Returns 200 with profile details including monitoring field")
    fun `should return profile with monitoring field`() {
      val profile = createRecipientProfileMonitoredBy(userId = 8L, sortCode = "112233", accountNumber = "12345678")

      webTestClient.get()
        .uri("/recipients/${profile.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.id").isEqualTo(profile.id!!.toInt())
        .jsonPath("$.sort_code").isEqualTo("112233")
        .jsonPath("$.account_number").isEqualTo("12345678")
        .jsonPath("$.monitoring").isEqualTo(true)
    }

    @Test
    @DisplayName("Returns monitoring=false when user does not monitor the profile")
    fun `should return monitoring false when not monitoring`() {
      val profile = createRecipientProfile()

      webTestClient.get()
        .uri("/recipients/${profile.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.monitoring").isEqualTo(false)
    }

    @Test
    @DisplayName("Returns 404 for non-existent profile")
    fun `should return 404 for non-existent profile`() {
      webTestClient.get()
        .uri("/recipients/99999/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("GET /security/recipients/{id}/disbursements/ (RecipientProfileDisbursementListTestCase)")
  inner class ListRecipientDisbursements {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      val profile = createRecipientProfile()
      webTestClient.get()
        .uri("/recipients/${profile.id}/disbursements/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("Returns disbursements matching the recipient profile bank account")
    fun `should return disbursements matching sort_code and account_number`() {
      val profile = createRecipientProfile(sortCode = "112233", accountNumber = "12345678")
      disbursementRepository.save(
        Disbursement(
          amount = 5000,
          method = DisbursementMethod.BANK_TRANSFER,
          resolution = uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution.PENDING,
          prison = "LEI",
          sortCode = "112233",
          accountNumber = "12345678",
        ),
      )
      disbursementRepository.save(
        Disbursement(
          amount = 2000,
          method = DisbursementMethod.BANK_TRANSFER,
          resolution = uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution.PENDING,
          prison = "LEI",
          sortCode = "445566",
          accountNumber = "99999999",
        ),
      )

      webTestClient.get()
        .uri("/recipients/${profile.id}/disbursements/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(5000)
    }

    @Test
    @DisplayName("Returns empty list when no matching disbursements")
    fun `should return empty list when no matching disbursements`() {
      val profile = createRecipientProfile(sortCode = "112233", accountNumber = "12345678")

      webTestClient.get()
        .uri("/recipients/${profile.id}/disbursements/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("POST /security/recipients/{id}/monitor/ and /unmonitor/ (SEC-105 to SEC-106)")
  inner class MonitorRecipient {

    @Test
    @DisplayName("Adds current user to monitoring for recipient")
    fun `should add user to monitoring`() {
      val profile = createRecipientProfile()

      webTestClient.post()
        .uri("/recipients/${profile.id}/monitor/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isNoContent

      val monitoringIds = jdbcTemplate.queryForList(
        """
        SELECT m.user_id FROM security_bankaccount_monitoring_users m
        JOIN security_banktransferrecipientdetails r
          ON r.recipient_bank_account_id = m.bankaccount_id
        WHERE r.recipient_id = ?
        """.trimIndent(),
        Long::class.java,
        profile.id,
      )
      assertThat(monitoringIds).contains(8L)
    }

    @Test
    @DisplayName("Removes current user from monitoring for recipient")
    fun `should remove user from monitoring`() {
      val saved = createRecipientProfileMonitoredBy(userId = 8L)

      webTestClient.post()
        .uri("/recipients/${saved.id}/unmonitor/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isNoContent

      val monitoringIds = jdbcTemplate.queryForList(
        """
        SELECT m.user_id FROM security_bankaccount_monitoring_users m
        JOIN security_banktransferrecipientdetails r
          ON r.recipient_bank_account_id = m.bankaccount_id
        WHERE r.recipient_id = ?
        """.trimIndent(),
        Long::class.java,
        saved.id,
      )
      assertThat(monitoringIds).doesNotContain(8L)
    }
  }
}

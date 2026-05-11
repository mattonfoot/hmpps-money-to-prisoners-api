package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

class SenderProfileResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var senderProfileRepository: SenderProfileRepository

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @BeforeEach
  fun setUp() {
    privateEstateBatchRepository.clearJoinTable()
    privateEstateBatchRepository.deleteAll()
    senderProfileRepository.deleteAll()
    creditRepository.deleteAll()
  }

  private fun createSenderProfile(): SenderProfile = senderProfileRepository.save(SenderProfile())

  /**
   * Persists a sender profile WITH a debit-card detail child and a monitoring
   * row linking that detail to the given userId — mirroring Django's storage
   * (monitoring lives on the detail child, not the parent profile).
   */
  private fun createSenderProfileMonitoredBy(userId: Long): SenderProfile = transactionTemplate.execute {
    val profile = senderProfileRepository.save(SenderProfile())
    jdbcTemplate.update(
      """
      INSERT INTO security_debitcardsenderdetails
        (created, modified, sender_id, postcode)
      VALUES (NOW(), NOW(), ?, '')
      """.trimIndent(),
      profile.id,
    )
    jdbcTemplate.update(
      """
      INSERT INTO security_debitcardsenderdetails_monitoring_users
        (debitcardsenderdetails_id, user_id)
      SELECT id, ? FROM security_debitcardsenderdetails WHERE sender_id = ?
      """.trimIndent(),
      userId,
      profile.id,
    )
    profile
  }!!

  @Nested
  @DisplayName("GET /security/senders/ (SEC-070 to SEC-080)")
  inner class ListSenderProfiles {

    @Test
    @DisplayName("SEC-070 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/senders/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-071 - Returns 403 for user without required role")
    fun `should return 403 without security role`() {
      webTestClient.get()
        .uri("/senders/")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("SEC-072 - Returns paginated list of sender profiles")
    fun `should return sender profiles`() {
      createSenderProfile()
      createSenderProfile()

      webTestClient.get()
        .uri("/senders/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results").isArray
    }
  }

  @Nested
  @DisplayName("POST /security/senders/{id}/monitor/ (SEC-060 to SEC-061)")
  inner class MonitorSender {

    @Test
    @DisplayName("SEC-060 - Adds current user to monitoring for sender")
    fun `should add user to monitoring`() {
      // monitor() walks debit-card detail children; the sender needs at least one.
      val profile = createSenderProfile()
      jdbcTemplate.update(
        """
        INSERT INTO security_debitcardsenderdetails (created, modified, sender_id, postcode)
        VALUES (NOW(), NOW(), ?, '')
        """.trimIndent(),
        profile.id,
      )

      webTestClient.post()
        .uri("/senders/${profile.id}/monitor/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isNoContent

      val monitoringUserIds = jdbcTemplate.queryForList(
        """
        SELECT m.user_id
        FROM security_debitcardsenderdetails_monitoring_users m
        JOIN security_debitcardsenderdetails d ON d.id = m.debitcardsenderdetails_id
        WHERE d.sender_id = ?
        """.trimIndent(),
        Long::class.java,
        profile.id,
      )
      assertThat(monitoringUserIds).contains(8L)
    }

    @Test
    @DisplayName("SEC-061 - Removes current user from monitoring for sender")
    fun `should remove user from monitoring`() {
      val saved = createSenderProfileMonitoredBy(userId = 8L)

      webTestClient.post()
        .uri("/senders/${saved.id}/unmonitor/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isNoContent

      val monitoringUserIds = jdbcTemplate.queryForList(
        """
        SELECT m.user_id
        FROM security_debitcardsenderdetails_monitoring_users m
        JOIN security_debitcardsenderdetails d ON d.id = m.debitcardsenderdetails_id
        WHERE d.sender_id = ?
        """.trimIndent(),
        Long::class.java,
        saved.id,
      )
      assertThat(monitoringUserIds).doesNotContain(8L)
    }
  }

  @Nested
  @DisplayName("GET /security/senders/ - filter tests (SenderProfileListTestCase)")
  inner class FilterSenderProfiles {

    @Test
    @DisplayName("Filters by monitoring=true returns only senders monitored by current user")
    fun `should filter by monitoring true`() {
      val monitoredProfile = createSenderProfileMonitoredBy(userId = 8L)
      createSenderProfile()
      createSenderProfile()

      webTestClient.get()
        .uri("/senders/?monitoring=true")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].id").isEqualTo(monitoredProfile.id!!.toInt())
    }

    @Test
    @DisplayName("Filters by monitoring=false returns senders NOT monitored by current user")
    fun `should filter by monitoring false`() {
      createSenderProfileMonitoredBy(userId = 8L)
      createSenderProfile()
      createSenderProfile()

      webTestClient.get()
        .uri("/senders/?monitoring=false")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("Returns monitoring field as true when current user monitors the sender")
    fun `should include monitoring field in detail view`() {
      val profile = createSenderProfileMonitoredBy(userId = 8L)

      webTestClient.get()
        .uri("/senders/${profile.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.id").isEqualTo(profile.id!!.toInt())
        .jsonPath("$.monitoring").isEqualTo(true)
    }

    @Test
    @DisplayName("Returns 404 for non-existent sender profile detail")
    fun `should return 404 for non-existent profile`() {
      webTestClient.get()
        .uri("/senders/99999/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("GET /security/senders/{id}/credits/ (SEC-075)")
  inner class ListSenderCredits {

    @Test
    @DisplayName("SEC-075 - Returns credits for sender profile")
    fun `should return credits for sender profile`() {
      // SenderProfile.credits is @OneToMany(mappedBy = "senderProfile"); the
      // owning side is Credit's sender_profile_id FK.
      val saved = senderProfileRepository.save(SenderProfile())
      val credit = Credit(amount = 5000, resolution = CreditResolution.PENDING)
      credit.senderProfile = saved
      creditRepository.save(credit)

      webTestClient.get()
        .uri("/senders/${saved.id}/credits/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(5000)
    }
  }
}

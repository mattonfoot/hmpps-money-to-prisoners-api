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

  @BeforeEach
  fun setUp() {
    senderProfileRepository.deleteAll()
    creditRepository.deleteAll()
  }

  private fun createSenderProfile(): SenderProfile = senderProfileRepository.save(SenderProfile())

  @Nested
  @DisplayName("GET /security/senders/ (SEC-070 to SEC-080)")
  inner class ListSenderProfiles {

    @Test
    @DisplayName("SEC-070 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/security/senders/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-071 - Returns 403 for user without required role")
    fun `should return 403 without security role`() {
      webTestClient.get()
        .uri("/security/senders/")
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
        .uri("/security/senders/")
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
      val profile = createSenderProfile()

      webTestClient.post()
        .uri("/security/senders/${profile.id}/monitor/")
        .headers(setAuthorisation(username = "security_user"))
        .exchange()
        .expectStatus().isNoContent

      val monitoringUsers = transactionTemplate.execute {
        val p = senderProfileRepository.findById(profile.id!!).get()
        p.monitoringUsers.toSet() // initialize lazy collection within tx
      }!!
      assertThat(monitoringUsers).contains("security_user")
    }

    @Test
    @DisplayName("SEC-061 - Removes current user from monitoring for sender")
    fun `should remove user from monitoring`() {
      val saved = transactionTemplate.execute {
        val p = SenderProfile()
        p.monitoringUsers.add("security_user")
        senderProfileRepository.save(p)
      }!!

      webTestClient.post()
        .uri("/security/senders/${saved.id}/unmonitor/")
        .headers(setAuthorisation(username = "security_user"))
        .exchange()
        .expectStatus().isNoContent

      val monitoringUsers = transactionTemplate.execute {
        val p = senderProfileRepository.findById(saved.id!!).get()
        p.monitoringUsers.toSet()
      }!!
      assertThat(monitoringUsers).doesNotContain("security_user")
    }
  }

  @Nested
  @DisplayName("GET /security/senders/ - filter tests (SenderProfileListTestCase)")
  inner class FilterSenderProfiles {

    @Test
    @DisplayName("Filters by monitoring=true returns only senders monitored by current user")
    fun `should filter by monitoring true`() {
      val monitoredProfile = transactionTemplate.execute {
        val p = SenderProfile()
        p.monitoringUsers.add("security_user")
        senderProfileRepository.save(p)
      }!!
      createSenderProfile()
      createSenderProfile()

      webTestClient.get()
        .uri("/security/senders/?monitoring=true")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].id").isEqualTo(monitoredProfile.id!!.toInt())
    }

    @Test
    @DisplayName("Filters by monitoring=false returns senders NOT monitored by current user")
    fun `should filter by monitoring false`() {
      transactionTemplate.execute {
        val p = SenderProfile()
        p.monitoringUsers.add("security_user")
        senderProfileRepository.save(p)
      }
      createSenderProfile()
      createSenderProfile()

      webTestClient.get()
        .uri("/security/senders/?monitoring=false")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("Returns monitoring field as true when current user monitors the sender")
    fun `should include monitoring field in detail view`() {
      val profile = transactionTemplate.execute {
        val p = SenderProfile()
        p.monitoringUsers.add("security_user")
        senderProfileRepository.save(p)
      }!!

      webTestClient.get()
        .uri("/security/senders/${profile.id}/")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
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
        .uri("/security/senders/99999/")
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
      val credit = creditRepository.save(Credit(amount = 5000, resolution = CreditResolution.PENDING))
      val profile = SenderProfile()
      profile.credits.add(credit)
      val saved = senderProfileRepository.save(profile)

      webTestClient.get()
        .uri("/security/senders/${saved.id}/credits/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(5000)
    }
  }
}

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
  ): RecipientProfile = recipientProfileRepository.save(RecipientProfile(sortCode = sortCode, accountNumber = accountNumber))

  @Nested
  @DisplayName("GET /security/recipients/ (RecipientProfileListTestCase)")
  inner class ListRecipientProfiles {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/security/recipients/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("Returns 403 for user without required role")
    fun `should return 403 without security role`() {
      webTestClient.get()
        .uri("/security/recipients/")
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
        .uri("/security/recipients/")
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
        .uri("/security/recipients/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("Filters by monitoring=true returns only recipients monitored by current user")
    fun `should filter by monitoring true`() {
      val monitoredProfile = transactionTemplate.execute {
        val p = RecipientProfile(sortCode = "112233", accountNumber = "11111111")
        p.monitoringUsers.add("security_user")
        recipientProfileRepository.save(p)
      }!!
      createRecipientProfile("445566", "22222222")

      webTestClient.get()
        .uri("/security/recipients/?monitoring=true")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].id").isEqualTo(monitoredProfile.id!!.toInt())
    }

    @Test
    @DisplayName("Filters by monitoring=false returns recipients NOT monitored by current user")
    fun `should filter by monitoring false`() {
      transactionTemplate.execute {
        val p = RecipientProfile(sortCode = "112233", accountNumber = "11111111")
        p.monitoringUsers.add("security_user")
        recipientProfileRepository.save(p)
      }
      createRecipientProfile("445566", "22222222")
      createRecipientProfile("778899", "33333333")

      webTestClient.get()
        .uri("/security/recipients/?monitoring=false")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
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
        .uri("/security/recipients/${profile.id}/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("Returns 200 with profile details including monitoring field")
    fun `should return profile with monitoring field`() {
      val profile = transactionTemplate.execute {
        val p = RecipientProfile(sortCode = "112233", accountNumber = "12345678")
        p.monitoringUsers.add("security_user")
        recipientProfileRepository.save(p)
      }!!

      webTestClient.get()
        .uri("/security/recipients/${profile.id}/")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
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
        .uri("/security/recipients/${profile.id}/")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.monitoring").isEqualTo(false)
    }

    @Test
    @DisplayName("Returns 404 for non-existent profile")
    fun `should return 404 for non-existent profile`() {
      webTestClient.get()
        .uri("/security/recipients/99999/")
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
        .uri("/security/recipients/${profile.id}/disbursements/")
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
          sortCode = "112233",
          accountNumber = "12345678",
        ),
      )
      disbursementRepository.save(
        Disbursement(
          amount = 2000,
          method = DisbursementMethod.BANK_TRANSFER,
          sortCode = "445566",
          accountNumber = "99999999",
        ),
      )

      webTestClient.get()
        .uri("/security/recipients/${profile.id}/disbursements/")
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
        .uri("/security/recipients/${profile.id}/disbursements/")
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
        .uri("/security/recipients/${profile.id}/monitor/")
        .headers(setAuthorisation(username = "security_user"))
        .exchange()
        .expectStatus().isNoContent

      val monitoringUsers = transactionTemplate.execute {
        val p = recipientProfileRepository.findById(profile.id!!).get()
        p.monitoringUsers.toSet()
      }!!
      assertThat(monitoringUsers).contains("security_user")
    }

    @Test
    @DisplayName("Removes current user from monitoring for recipient")
    fun `should remove user from monitoring`() {
      val saved = transactionTemplate.execute {
        val p = RecipientProfile(sortCode = "112233", accountNumber = "12345678")
        p.monitoringUsers.add("security_user")
        recipientProfileRepository.save(p)
      }!!

      webTestClient.post()
        .uri("/security/recipients/${saved.id}/unmonitor/")
        .headers(setAuthorisation(username = "security_user"))
        .exchange()
        .expectStatus().isNoContent

      val monitoringUsers = transactionTemplate.execute {
        val p = recipientProfileRepository.findById(saved.id!!).get()
        p.monitoringUsers.toSet()
      }!!
      assertThat(monitoringUsers).doesNotContain("security_user")
    }
  }
}

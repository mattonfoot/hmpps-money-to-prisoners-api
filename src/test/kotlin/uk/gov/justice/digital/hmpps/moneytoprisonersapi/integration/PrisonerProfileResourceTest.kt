package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository

class PrisonerProfileResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  @BeforeEach
  fun setUp() {
    prisonerProfileRepository.deleteAll()
    creditRepository.deleteAll()
  }

  private fun createPrisonerProfile(prisonerNumber: String = "A1234BC"): PrisonerProfile = prisonerProfileRepository.save(PrisonerProfile(prisonerNumber = prisonerNumber, prisonerName = "John Smith"))

  @Nested
  @DisplayName("GET /security/prisoners/ (SEC-090 to SEC-098)")
  inner class ListPrisonerProfiles {

    @Test
    @DisplayName("SEC-090 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/security/prisoners/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-091 - Returns 403 for user without required role")
    fun `should return 403 without security role`() {
      webTestClient.get()
        .uri("/security/prisoners/")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("SEC-092 - Returns paginated list of prisoner profiles")
    fun `should return prisoner profiles`() {
      createPrisonerProfile("A1234BC")
      createPrisonerProfile("B5678DE")

      webTestClient.get()
        .uri("/security/prisoners/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("POST /security/prisoners/{id}/monitor/ (SEC-062 to SEC-063)")
  inner class MonitorPrisoner {

    @Test
    @DisplayName("SEC-062 - Adds current user to monitoring for prisoner")
    fun `should add user to monitoring`() {
      val profile = createPrisonerProfile()

      webTestClient.post()
        .uri("/security/prisoners/${profile.id}/monitor/")
        .headers(setAuthorisation(username = "security_user"))
        .exchange()
        .expectStatus().isNoContent

      val monitoringUsers = transactionTemplate.execute {
        val p = prisonerProfileRepository.findById(profile.id!!).get()
        p.monitoringUsers.toSet()
      }!!
      assertThat(monitoringUsers).contains("security_user")
    }

    @Test
    @DisplayName("SEC-063 - Removes current user from monitoring for prisoner")
    fun `should remove user from monitoring`() {
      val saved = transactionTemplate.execute {
        val p = PrisonerProfile(prisonerNumber = "A1234BC", prisonerName = "John Smith")
        p.monitoringUsers.add("security_user")
        prisonerProfileRepository.save(p)
      }!!

      webTestClient.post()
        .uri("/security/prisoners/${saved.id}/unmonitor/")
        .headers(setAuthorisation(username = "security_user"))
        .exchange()
        .expectStatus().isNoContent

      val monitoringUsers = transactionTemplate.execute {
        val p = prisonerProfileRepository.findById(saved.id!!).get()
        p.monitoringUsers.toSet()
      }!!
      assertThat(monitoringUsers).doesNotContain("security_user")
    }
  }

  @Nested
  @DisplayName("GET /security/prisoners/ - filter tests (PrisonerProfileListTestCase)")
  inner class FilterPrisonerProfiles {

    @Test
    @DisplayName("Filters by monitoring=true returns only prisoners monitored by current user")
    fun `should filter by monitoring true`() {
      val monitoredProfile = transactionTemplate.execute {
        val p = PrisonerProfile(prisonerNumber = "MONITORED1", prisonerName = "Monitored Prisoner")
        p.monitoringUsers.add("security_user")
        prisonerProfileRepository.save(p)
      }!!
      createPrisonerProfile("A1111BC")
      createPrisonerProfile("B2222DE")

      webTestClient.get()
        .uri("/security/prisoners/?monitoring=true")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].id").isEqualTo(monitoredProfile.id!!.toInt())
    }

    @Test
    @DisplayName("Filters by monitoring=false returns prisoners NOT monitored by current user")
    fun `should filter by monitoring false`() {
      transactionTemplate.execute {
        val p = PrisonerProfile(prisonerNumber = "MONITORED1", prisonerName = "Monitored Prisoner")
        p.monitoringUsers.add("security_user")
        prisonerProfileRepository.save(p)
      }
      createPrisonerProfile("A1111BC")
      createPrisonerProfile("B2222DE")

      webTestClient.get()
        .uri("/security/prisoners/?monitoring=false")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("Returns monitoring field in detail view")
    fun `should return monitoring field on detail endpoint`() {
      val profile = transactionTemplate.execute {
        val p = PrisonerProfile(prisonerNumber = "A1234BC", prisonerName = "John Smith")
        p.monitoringUsers.add("security_user")
        prisonerProfileRepository.save(p)
      }!!

      webTestClient.get()
        .uri("/security/prisoners/${profile.id}/")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.id").isEqualTo(profile.id!!.toInt())
        .jsonPath("$.monitoring").isEqualTo(true)
    }

    @Test
    @DisplayName("Returns 404 for non-existent prisoner profile detail")
    fun `should return 404 for non-existent profile`() {
      webTestClient.get()
        .uri("/security/prisoners/99999/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    @DisplayName("Filters by simple_search matches prisoner_name or prisoner_number")
    fun `should filter by simple_search matching prisoner name`() {
      createPrisonerProfile("A1234BC").also {
        prisonerProfileRepository.save(it.apply { prisonerName = "JOHN SMITH" })
      }
      createPrisonerProfile("B5678DE").also {
        prisonerProfileRepository.save(it.apply { prisonerName = "JANE DOE" })
      }

      webTestClient.get()
        .uri("/security/prisoners/?simple_search=JOHN")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_name").isEqualTo("JOHN SMITH")
    }
  }

  @Nested
  @DisplayName("GET /security/prisoners/{id}/credits/ (SEC-093)")
  inner class ListPrisonerCredits {

    @Test
    @DisplayName("SEC-093 - Returns credits for prisoner profile")
    fun `should return credits for prisoner profile`() {
      val credit = creditRepository.save(
        Credit(amount = 3000, prisonerNumber = "A1234BC", resolution = CreditResolution.PENDING),
      )
      val profile = PrisonerProfile(prisonerNumber = "A1234BC", prisonerName = "John Smith")
      profile.credits.add(credit)
      val saved = prisonerProfileRepository.save(profile)

      webTestClient.get()
        .uri("/security/prisoners/${saved.id}/credits/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(3000)
    }
  }
}

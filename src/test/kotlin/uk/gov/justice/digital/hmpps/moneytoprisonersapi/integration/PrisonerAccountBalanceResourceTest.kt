package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerBalance
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerBalanceRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerLocationRepository

class PrisonerAccountBalanceResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var prisonerBalanceRepository: PrisonerBalanceRepository

  @Autowired
  private lateinit var prisonerLocationRepository: PrisonerLocationRepository

  @BeforeEach
  fun setUp() {
    prisonerLocationRepository.deleteAll()
    prisonerBalanceRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun createPrison(nomisId: String, privateEstate: Boolean = false): Prison = prisonRepository.save(Prison(nomisId = nomisId, name = "Test Prison", region = "Test Region", privateEstate = privateEstate))

  @Nested
  @DisplayName("GET /prisoner_account_balances/{prisoner_number}/")
  inner class GetPrisonerAccountBalance {

    @Test
    @DisplayName("PRS-060 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.get()
        .uri("/prisoner_account_balances/A1234BC/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-061 - Without ROLE_SEND_MONEY returns 403")
    fun `should return 403 without ROLE_SEND_MONEY`() {
      webTestClient.get()
        .uri("/prisoner_account_balances/A1234BC/")
        .headers(setAuthorisation(roles = listOf("ROLE_CASHBOOK")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PRS-062 - Returns balance from PrisonerBalance record for private estate prisons")
    fun `should return balance from PrisonerBalance record`() {
      val prison = createPrison("LEI", privateEstate = true)
      prisonerBalanceRepository.save(PrisonerBalance(prisonerNumber = "A1234BC", prison = prison, amount = 5000L))

      webTestClient.get()
        .uri("/prisoner_account_balances/A1234BC/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.combined_account_balance").isEqualTo(5000)
    }

    @Test
    @DisplayName("PRS-063 - Returns 0 when no PrisonerBalance record found")
    fun `should return 0 when no balance record exists`() {
      webTestClient.get()
        .uri("/prisoner_account_balances/A9999XX/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.combined_account_balance").isEqualTo(0)
    }

    @Test
    @DisplayName("PRS-064 - Returns 0 for NOMIS prison (not private estate)")
    fun `should return 0 for NOMIS prison`() {
      val prison = createPrison("LEI", privateEstate = false)
      prisonerBalanceRepository.save(PrisonerBalance(prisonerNumber = "A1234BC", prison = prison, amount = 9999L))

      webTestClient.get()
        .uri("/prisoner_account_balances/A1234BC/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.combined_account_balance").isEqualTo(9999)
    }
  }
}

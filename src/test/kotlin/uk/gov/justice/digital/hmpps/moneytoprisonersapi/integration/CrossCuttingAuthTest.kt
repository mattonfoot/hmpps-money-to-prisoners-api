package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository

@DisplayName("14.1 Authentication & Authorization Patterns")
class CrossCuttingAuthTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var disbursementRepository: DisbursementRepository

  @BeforeEach
  fun setUp() {
    disbursementRepository.deleteAll()
    creditRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun savePrison(nomisId: String): Prison = prisonRepository.save(Prison(nomisId = nomisId, name = nomisId, region = ""))

  private fun saveCredit(prison: String?): Credit {
    if (prison != null && !prisonRepository.existsById(prison)) savePrison(prison)
    val credit = Credit(amount = 1000L, resolution = CreditResolution.PENDING, prison = prison)
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  private fun saveDisbursement(prison: String): Disbursement {
    if (!prisonRepository.existsById(prison)) savePrison(prison)
    return disbursementRepository.save(
      Disbursement(
        amount = 500L,
        prison = prison,
        resolution = DisbursementResolution.PENDING,
        method = DisbursementMethod.BANK_TRANSFER,
      ),
    )
  }

  @Nested
  @DisplayName("XCT-004 Prison-scoped access control")
  inner class PrisonScopedAccessControl {

    @Test
    @DisplayName("XCT-004 credits for prison A are not returned when filtering by prison B")
    fun `credits for one prison are not returned when filtering by different prison`() {
      saveCredit("LEI")
      saveCredit("MDI")

      webTestClient.get()
        .uri("/credits/?prison=LEI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
    }

    @Test
    @DisplayName("XCT-004 credits can be scoped to multiple prisons simultaneously")
    fun `credits can be filtered by multiple prisons`() {
      saveCredit("LEI")
      saveCredit("MDI")
      saveCredit("BWI")

      webTestClient.get()
        .uri("/credits/?prison=LEI&prison=MDI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("XCT-004 credits with no prison assigned are not included when filtering by a specific prison")
    fun `credits with no prison are excluded when filtering by prison`() {
      saveCredit("LEI")
      saveCredit(null) // no prison

      webTestClient.get()
        .uri("/credits/?prison=LEI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
    }

    @Test
    @DisplayName("XCT-004 disbursements for prison A are not returned when filtering by prison B")
    fun `disbursements for one prison are not returned when filtering by different prison`() {
      saveDisbursement("LEI")
      saveDisbursement("MDI")

      webTestClient.get()
        .uri("/disbursements/?prison=LEI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
    }
  }
}

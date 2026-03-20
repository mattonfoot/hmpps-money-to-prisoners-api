package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Balance
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BalanceRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import java.math.BigInteger
import java.time.LocalDate

@DisplayName("14.2 Pagination")
class CrossCuttingPaginationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var balanceRepository: BalanceRepository

  @Autowired
  private lateinit var disbursementRepository: DisbursementRepository

  @BeforeEach
  fun setUp() {
    disbursementRepository.deleteAll()
    creditRepository.deleteAll()
    balanceRepository.deleteAll()
  }

  private fun saveCredit(): Credit {
    val credit = Credit(amount = 1000L, resolution = CreditResolution.PENDING)
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  private fun saveDisbursement(): Disbursement = disbursementRepository.save(
    Disbursement(
      amount = 500L,
      prison = "LEI",
      resolution = DisbursementResolution.PENDING,
      method = DisbursementMethod.BANK_TRANSFER,
    ),
  )

  @Nested
  @DisplayName("XCT-010 Standard pagination on list endpoints")
  inner class StandardPagination {

    @Test
    @DisplayName("XCT-010 GET /credits/ returns paginated response with count, next, previous, results")
    fun `credits list endpoint returns standard paginated response format`() {
      saveCredit()
      saveCredit()

      webTestClient.get()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.next").isEmpty
        .jsonPath("$.previous").isEmpty
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(2)
    }

    @Test
    @DisplayName("XCT-010 GET /balances/ returns paginated response with count, next, previous, results")
    fun `balances list endpoint returns standard paginated response format`() {
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(1000), date = LocalDate.of(2024, 1, 1)))

      webTestClient.get()
        .uri("/balances/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.next").isEmpty
        .jsonPath("$.previous").isEmpty
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(1)
    }

    @Test
    @DisplayName("XCT-010 GET /disbursements/ returns paginated response with count, next, previous, results")
    fun `disbursements list endpoint returns standard paginated response format`() {
      saveDisbursement()
      saveDisbursement()
      saveDisbursement()

      webTestClient.get()
        .uri("/disbursements/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(3)
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(3)
    }

    @Test
    @DisplayName("XCT-010 count field reflects total number of records in database")
    fun `count field matches total records`() {
      repeat(5) { saveCredit() }

      webTestClient.get()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(5)
        .jsonPath("$.results.length()").isEqualTo(5)
    }

    @Test
    @DisplayName("XCT-010 empty results return count=0 with empty results array")
    fun `empty list returns count zero and empty results`() {
      webTestClient.get()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("XCT-011 Some endpoints are explicitly non-paginated")
  inner class NonPaginatedEndpoints {

    @Test
    @DisplayName("XCT-011 GET /batches/ returns a plain array (not a paginated object)")
    fun `batches list endpoint returns array not paginated object`() {
      webTestClient.get()
        .uri("/batches/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isArray
    }

    @Test
    @DisplayName("XCT-011 GET /security/monitored-email-addresses/ returns a plain array (not a paginated object)")
    fun `monitored email addresses list endpoint returns plain array`() {
      webTestClient.get()
        .uri("/security/monitored-email-addresses/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isArray
    }
  }
}

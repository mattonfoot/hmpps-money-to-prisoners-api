package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Balance
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BalanceRepository
import java.math.BigInteger
import java.time.LocalDate

class BalanceResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var balanceRepository: BalanceRepository

  @BeforeEach
  fun setUp() {
    balanceRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /balances/")
  inner class ListBalances {

    @Test
    @DisplayName("ACC-011 - Unauthenticated request returns 401")
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/balances/")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    @DisplayName("ACC-012 - Any authenticated user can list balances")
    fun `should return 200 for any authenticated user`() {
      webTestClient.get()
        .uri("/balances/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
    }

    @Test
    @DisplayName("ACC-010 - GET /balances/ returns 200 with paginated response")
    fun `should return paginated response format`() {
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(12345), date = LocalDate.of(2024, 1, 15)))

      webTestClient.get()
        .uri("/balances/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.next").isEmpty
        .jsonPath("$.previous").isEmpty
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(1)
    }

    @Test
    @DisplayName("ACC-017 - Empty database returns empty results")
    fun `should return empty results when no balances exist`() {
      webTestClient.get()
        .uri("/balances/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(0)
    }

    @Test
    @DisplayName("ACC-018 - Response includes all fields")
    fun `should include all balance fields in response`() {
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(98765), date = LocalDate.of(2024, 3, 20)))

      webTestClient.get()
        .uri("/balances/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.results[0].id").isNotEmpty
        .jsonPath("$.results[0].closing_balance").isEqualTo(98765)
        .jsonPath("$.results[0].date").isEqualTo("2024-03-20")
        .jsonPath("$.results[0].created").isNotEmpty
        .jsonPath("$.results[0].modified").isNotEmpty
    }

    @Test
    @DisplayName("ACC-013 - Results ordered newest-first")
    fun `should return results ordered by date descending`() {
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(100), date = LocalDate.of(2024, 1, 1)))
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(200), date = LocalDate.of(2024, 1, 3)))
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(300), date = LocalDate.of(2024, 1, 2)))

      webTestClient.get()
        .uri("/balances/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.results[0].date").isEqualTo("2024-01-03")
        .jsonPath("$.results[1].date").isEqualTo("2024-01-02")
        .jsonPath("$.results[2].date").isEqualTo("2024-01-01")
    }

    @Test
    @DisplayName("ACC-014 - Filter by date__lt returns balances before given date")
    fun `should filter balances before given date`() {
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(100), date = LocalDate.of(2024, 1, 1)))
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(200), date = LocalDate.of(2024, 1, 15)))
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(300), date = LocalDate.of(2024, 1, 31)))

      webTestClient.get()
        .uri("/balances/?date__lt=2024-01-20")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results[0].date").isEqualTo("2024-01-15")
        .jsonPath("$.results[1].date").isEqualTo("2024-01-01")
    }

    @Test
    @DisplayName("ACC-015 - Filter by date__gte returns balances on or after given date")
    fun `should filter balances on or after given date`() {
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(100), date = LocalDate.of(2024, 1, 1)))
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(200), date = LocalDate.of(2024, 1, 15)))
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(300), date = LocalDate.of(2024, 1, 31)))

      webTestClient.get()
        .uri("/balances/?date__gte=2024-01-15")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results[0].date").isEqualTo("2024-01-31")
        .jsonPath("$.results[1].date").isEqualTo("2024-01-15")
    }

    @Test
    @DisplayName("ACC-016 - Combined date filters return correct range")
    fun `should filter with combined date range`() {
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(100), date = LocalDate.of(2024, 1, 1)))
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(200), date = LocalDate.of(2024, 1, 15)))
      balanceRepository.save(Balance(closingBalance = BigInteger.valueOf(300), date = LocalDate.of(2024, 1, 31)))

      webTestClient.get()
        .uri("/balances/?date__gte=2024-01-10&date__lt=2024-01-20")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].date").isEqualTo("2024-01-15")
    }
  }
}

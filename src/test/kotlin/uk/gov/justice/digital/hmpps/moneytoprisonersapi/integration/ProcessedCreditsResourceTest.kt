package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import java.time.LocalDateTime

class ProcessedCreditsResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var logRepository: LogRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @BeforeEach
  fun setUp() {
    logRepository.deleteAll()
    creditRepository.deleteAll()
    prisonRepository.deleteAll()
    prisonRepository.save(Prison(nomisId = "LEI", name = "Leeds", region = "Yorkshire"))
  }

  private fun createAndSaveCreditedCredit(
    amount: Long = 1000,
    prisonerNumber: String? = "A1234BC",
    prisonerName: String? = "John Smith",
    owner: String? = "clerk1",
  ): Credit {
    val credit = Credit(
      amount = amount,
      prisonerNumber = prisonerNumber,
      prisonerName = prisonerName,
      prison = "LEI",
      resolution = CreditResolution.CREDITED,
      owner = owner,
    )
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  private fun addCreditedLog(credit: Credit, userId: String = "clerk1", loggedAt: LocalDateTime = LocalDateTime.now()): Log {
    val log = Log(action = LogAction.CREDITED, credit = credit, userId = userId)
    log.created = loggedAt
    return logRepository.save(log)
  }

  @Nested
  @DisplayName("GET /credits/processed/ (CRD-150 to CRD-157)")
  inner class ProcessedCredits {

    @Test
    @DisplayName("CRD-157 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/credits/processed/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-150 - GET /credits/processed/ returns 200 with list")
    fun `should return 200 with list`() {
      val credit = createAndSaveCreditedCredit()
      addCreditedLog(credit, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 15, 10, 0))

      webTestClient.get()
        .uri("/credits/processed/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isArray
    }

    @Test
    @DisplayName("CRD-150 - Empty results when no credited credits")
    fun `should return empty list when no credited credits`() {
      webTestClient.get()
        .uri("/credits/processed/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isArray
        .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    @DisplayName("CRD-151 - Groups by logged_at date (UTC) and owner")
    fun `should group by logged_at date and owner`() {
      val credit1 = createAndSaveCreditedCredit(owner = "clerk1")
      val credit2 = createAndSaveCreditedCredit(owner = "clerk1")
      val credit3 = createAndSaveCreditedCredit(owner = "clerk2")
      val credit4 = createAndSaveCreditedCredit(owner = "clerk1")

      // Same day, same owner → same group
      addCreditedLog(credit1, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 15, 10, 0))
      addCreditedLog(credit2, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 15, 14, 0))
      // Same day, different owner → different group
      addCreditedLog(credit3, userId = "clerk2", loggedAt = LocalDateTime.of(2024, 3, 15, 11, 0))
      // Different day, same owner → different group
      addCreditedLog(credit4, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 16, 9, 0))

      webTestClient.get()
        .uri("/credits/processed/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(3)
    }

    @Test
    @DisplayName("CRD-152 - Returns count, total, comment_count per group")
    fun `should return count total and comment_count`() {
      val credit1 = createAndSaveCreditedCredit(amount = 1000, owner = "clerk1")
      val credit2 = createAndSaveCreditedCredit(amount = 2500, owner = "clerk1")
      addCreditedLog(credit1, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 15, 10, 0))
      addCreditedLog(credit2, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 15, 12, 0))

      val result = webTestClient.get()
        .uri("/credits/processed/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].count").isEqualTo(2)
        .jsonPath("$[0].total").isEqualTo(3500)
        .jsonPath("$[0].comment_count").isEqualTo(0)
    }

    @Test
    @DisplayName("CRD-153 - owner_name is Unknown for unknown users")
    fun `should return Unknown for owner_name`() {
      val credit = createAndSaveCreditedCredit(owner = "unknownuser")
      addCreditedLog(credit, userId = "unknownuser", loggedAt = LocalDateTime.of(2024, 3, 15, 10, 0))

      webTestClient.get()
        .uri("/credits/processed/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].owner").isEqualTo("unknownuser")
        .jsonPath("$[0].owner_name").isEqualTo("Unknown")
    }

    @Test
    @DisplayName("CRD-154 - Only returns groups for credits with LogAction.CREDITED")
    fun `should only include groups with CREDITED log`() {
      // A credit with no CREDITED log should not appear
      val creditNoLog = createAndSaveCreditedCredit(owner = "clerk1")
      // A credit with a REVIEWED log but no CREDITED log should not appear
      val creditReviewedOnly = createAndSaveCreditedCredit(owner = "clerk1")
      logRepository.save(Log(action = LogAction.REVIEWED, credit = creditReviewedOnly, userId = "clerk1"))
      // Only this one has a CREDITED log
      val creditWithLog = createAndSaveCreditedCredit(owner = "clerk1")
      addCreditedLog(creditWithLog, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 15, 10, 0))

      webTestClient.get()
        .uri("/credits/processed/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-155 - Supports prisoner_name filter")
    fun `should support prisoner_name filter`() {
      val credit1 = createAndSaveCreditedCredit(prisonerName = "John Smith", owner = "clerk1")
      val credit2 = createAndSaveCreditedCredit(prisonerName = "Jane Doe", owner = "clerk1")
      addCreditedLog(credit1, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 15, 10, 0))
      addCreditedLog(credit2, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 15, 11, 0))

      webTestClient.get()
        .uri("/credits/processed/?prisoner_name=John")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-156 - Ordered by logged_at descending")
    fun `should order by logged_at descending`() {
      val credit1 = createAndSaveCreditedCredit(owner = "clerk1")
      val credit2 = createAndSaveCreditedCredit(owner = "clerk2")
      addCreditedLog(credit1, userId = "clerk1", loggedAt = LocalDateTime.of(2024, 3, 15, 10, 0))
      addCreditedLog(credit2, userId = "clerk2", loggedAt = LocalDateTime.of(2024, 3, 16, 10, 0))

      webTestClient.get()
        .uri("/credits/processed/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].owner").isEqualTo("clerk2")
        .jsonPath("$[1].owner").isEqualTo("clerk1")
    }
  }
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import java.time.LocalDate
import java.time.LocalDateTime

class CreditResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @BeforeEach
  fun setUp() {
    creditRepository.deleteAll()
  }

  private fun createAndSaveCredit(
    amount: Long = 1000,
    prisonerNumber: String? = "A1234BC",
    prisonerName: String? = "John Smith",
    prisonerDob: LocalDate? = LocalDate.of(1990, 1, 15),
    prison: String? = "LEI",
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    reviewed: Boolean = false,
    reconciled: Boolean = false,
    receivedAt: LocalDateTime? = LocalDateTime.of(2024, 3, 15, 10, 0),
    owner: String? = null,
    incompleteSenderInfo: Boolean = false,
    source: CreditSource = CreditSource.BANK_TRANSFER,
  ): Credit {
    val credit = Credit(
      amount = amount,
      prisonerNumber = prisonerNumber,
      prisonerName = prisonerName,
      prisonerDob = prisonerDob,
      prison = prison,
      resolution = resolution,
      blocked = blocked,
      reviewed = reviewed,
      reconciled = reconciled,
      receivedAt = receivedAt,
      owner = owner,
      incompleteSenderInfo = incompleteSenderInfo,
    )
    credit.source = source
    return creditRepository.save(credit)
  }

  @Nested
  @DisplayName("GET /credits/")
  inner class ListCredits {

    @Test
    @DisplayName("CRD-021 - Unauthenticated request returns 401")
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/credits/")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    @DisplayName("CRD-020 - GET /credits/ returns 200 with paginated response")
    fun `should return paginated response format`() {
      createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")

      webTestClient.get()
        .uri("/credits/")
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
    @DisplayName("CRD-020 - Empty database returns empty results")
    fun `should return empty results when no credits exist`() {
      webTestClient.get()
        .uri("/credits/")
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
    @DisplayName("CRD-020 - Response includes all credit fields")
    fun `should include all credit fields in response`() {
      createAndSaveCredit(
        amount = 5000,
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        prison = "LEI",
        resolution = CreditResolution.PENDING,
        receivedAt = LocalDateTime.of(2024, 3, 15, 10, 30),
      )

      webTestClient.get()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.results[0].id").isNotEmpty
        .jsonPath("$.results[0].amount").isEqualTo(5000)
        .jsonPath("$.results[0].prisoner_number").isEqualTo("A1234BC")
        .jsonPath("$.results[0].prisoner_name").isEqualTo("John Smith")
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
        .jsonPath("$.results[0].resolution").isEqualTo("PENDING")
        .jsonPath("$.results[0].source").isEqualTo("BANK_TRANSFER")
        .jsonPath("$.results[0].status").isEqualTo("credit_pending")
        .jsonPath("$.results[0].blocked").isEqualTo(false)
        .jsonPath("$.results[0].reviewed").isEqualTo(false)
        .jsonPath("$.results[0].reconciled").isEqualTo(false)
        .jsonPath("$.results[0].received_at").isNotEmpty
        .jsonPath("$.results[0].created").isNotEmpty
        .jsonPath("$.results[0].modified").isNotEmpty
    }

    @Test
    @DisplayName("CRD-010 - Excludes initial and failed credits")
    fun `should exclude initial and failed credits`() {
      createAndSaveCredit(resolution = CreditResolution.INITIAL)
      createAndSaveCredit(resolution = CreditResolution.FAILED)
      createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Status (CRD-030 to CRD-037)")
  inner class StatusFilters {

    @Test
    @DisplayName("CRD-030 - Filter status=credit_pending")
    fun `should filter by status credit_pending`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=CREDIT_PENDING")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("credit_pending")
    }

    @Test
    @DisplayName("CRD-031 - Filter status=credited")
    fun `should filter by status credited`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=CREDITED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("credited")
    }

    @Test
    @DisplayName("CRD-032 - Filter status=refund_pending")
    fun `should filter by status refund_pending`() {
      createAndSaveCredit(prison = null, resolution = CreditResolution.PENDING, blocked = false, incompleteSenderInfo = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=REFUND_PENDING")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("refund_pending")
    }

    @Test
    @DisplayName("CRD-033 - Filter status=refunded")
    fun `should filter by status refunded`() {
      createAndSaveCredit(resolution = CreditResolution.REFUNDED)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=REFUNDED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("refunded")
    }

    @Test
    @DisplayName("CRD-035 - Invalid status returns empty set")
    fun `should return empty for status with no matches`() {
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=FAILED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("CRD-036 - Filter valid=true returns credit_pending or credited")
    fun `should filter valid true`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prison = null, resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?valid=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-037 - Filter valid=false returns non-valid credits")
    fun `should filter valid false`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prison = null, resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?valid=false")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Prison (CRD-040 to CRD-046)")
  inner class PrisonFilters {

    @Test
    @DisplayName("CRD-040 - Filter prison={nomis_id}")
    fun `should filter by exact prison id`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "MDI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison=LEI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
    }

    @Test
    @DisplayName("CRD-042 - Filter prison__isnull=True")
    fun `should filter credits with no prison`() {
      createAndSaveCredit(prison = null, resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison__isnull=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEmpty
    }

    @Test
    @DisplayName("CRD-046 - Invalid prison ID returns empty set")
    fun `should return empty for non-existent prison`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison=NONEXISTENT")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Amount (CRD-050 to CRD-057)")
  inner class AmountFilters {

    @Test
    @DisplayName("CRD-050 - Filter amount={exact}")
    fun `should filter by exact amount`() {
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2000, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount=1000")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(1000)
    }

    @Test
    @DisplayName("CRD-051 - Filter amount__gte")
    fun `should filter by minimum amount`() {
      createAndSaveCredit(amount = 500, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1500, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount__gte=1000")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-052 - Filter amount__lte")
    fun `should filter by maximum amount`() {
      createAndSaveCredit(amount = 500, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1500, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount__lte=1000")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-057 - Multiple amount filters combine with AND")
    fun `should combine amount filters with AND`() {
      createAndSaveCredit(amount = 500, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1500, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount__gte=800&amount__lte=1200")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(1000)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Other (CRD-080 to CRD-085)")
  inner class OtherFilters {

    @Test
    @DisplayName("CRD-080 - Filter prisoner_name case-insensitive substring")
    fun `should filter by prisoner name`() {
      createAndSaveCredit(prisonerName = "John Smith", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerName = "Jane Doe", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?prisoner_name=john")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_name").isEqualTo("John Smith")
    }

    @Test
    @DisplayName("CRD-081 - Filter prisoner_number exact match")
    fun `should filter by prisoner number`() {
      createAndSaveCredit(prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerNumber = "B5678DE", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?prisoner_number=A1234BC")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_number").isEqualTo("A1234BC")
    }

    @Test
    @DisplayName("CRD-082 - Filter by user (owner)")
    fun `should filter by owner`() {
      createAndSaveCredit(owner = "clerk1", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(owner = "clerk2", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?user=clerk1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-083 - Filter by resolution")
    fun `should filter by resolution`() {
      createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?resolution=CREDITED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].resolution").isEqualTo("CREDITED")
    }

    @Test
    @DisplayName("CRD-084 - Filter reviewed=true")
    fun `should filter by reviewed flag`() {
      createAndSaveCredit(reviewed = true, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(reviewed = false, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?reviewed=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].reviewed").isEqualTo(true)
    }

    @Test
    @DisplayName("CRD-085 - Filter received_at__gte/lt datetime range")
    fun `should filter by received_at range`() {
      createAndSaveCredit(receivedAt = LocalDateTime.of(2024, 1, 1, 10, 0), resolution = CreditResolution.CREDITED)
      createAndSaveCredit(receivedAt = LocalDateTime.of(2024, 2, 15, 10, 0), resolution = CreditResolution.CREDITED)
      createAndSaveCredit(receivedAt = LocalDateTime.of(2024, 3, 30, 10, 0), resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?received_at__gte=2024-02-01T00:00:00&received_at__lt=2024-03-01T00:00:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("Method Not Allowed")
  inner class MethodNotAllowed {

    @Test
    @DisplayName("PUT /credits/ returns 405")
    fun `should return method not allowed for PUT`() {
      webTestClient.put()
        .uri("/credits/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isEqualTo(405)
    }

    @Test
    @DisplayName("PATCH /credits/ returns 405")
    fun `should return method not allowed for PATCH`() {
      webTestClient.patch()
        .uri("/credits/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isEqualTo(405)
    }

    @Test
    @DisplayName("DELETE /credits/ returns 405")
    fun `should return method not allowed for DELETE`() {
      webTestClient.delete()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isEqualTo(405)
    }
  }
}

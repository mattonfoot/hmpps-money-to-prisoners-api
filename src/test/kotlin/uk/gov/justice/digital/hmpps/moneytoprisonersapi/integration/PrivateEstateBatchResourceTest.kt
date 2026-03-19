package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import java.time.LocalDate

class PrivateEstateBatchResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @BeforeEach
  fun setUp() {
    privateEstateBatchRepository.deleteAll()
    creditRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun createPrivatePrison(nomisId: String = "PRV"): Prison {
    val prison = Prison(nomisId = nomisId, name = "Private Prison", region = "South")
    prison.privateEstate = true
    return prisonRepository.save(prison)
  }

  private fun createPublicPrison(nomisId: String = "PUB"): Prison {
    val prison = Prison(nomisId = nomisId, name = "Public Prison", region = "North")
    prison.privateEstate = false
    return prisonRepository.save(prison)
  }

  private fun createAndSaveCredit(
    prison: String? = "PRV",
    resolution: CreditResolution = CreditResolution.PENDING,
  ): Credit {
    val credit = Credit(
      amount = 1000,
      prisonerNumber = "A1234BC",
      prisonerName = "John Smith",
      prison = prison,
      resolution = resolution,
    )
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  private fun createPrivateEstateBatch(
    ref: String,
    prison: Prison,
    date: LocalDate,
    credits: List<Credit> = emptyList(),
    totalAmount: Long = 0,
  ): PrivateEstateBatch {
    val batch = PrivateEstateBatch(
      ref = ref,
      prison = prison.nomisId,
      date = date,
      totalAmount = totalAmount,
    )
    batch.credits.addAll(credits)
    return privateEstateBatchRepository.save(batch)
  }

  @Nested
  @DisplayName("GET /private-estate-batches/ (CRD-180 to CRD-182)")
  inner class ListPrivateEstateBatches {

    @Test
    @DisplayName("CRD-180 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated GET`() {
      webTestClient.get()
        .uri("/private-estate-batches/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-180 - GET /private-estate-batches/ returns list")
    fun `should return list of private estate batches`() {
      val prison = createPrivatePrison()
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))

      webTestClient.get()
        .uri("/private-estate-batches/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isArray
        .jsonPath("$.length()").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-181 - Filter by date")
    fun `should filter by date`() {
      val prison = createPrivatePrison()
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))
      createPrivateEstateBatch("PRV/2024-03-16", prison, LocalDate.of(2024, 3, 16))

      webTestClient.get()
        .uri("/private-estate-batches/?date=2024-03-15")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].ref").isEqualTo("PRV/2024-03-15")
    }

    @Test
    @DisplayName("CRD-181 - Filter by date__gte")
    fun `should filter by date__gte`() {
      val prison = createPrivatePrison()
      createPrivateEstateBatch("PRV/2024-03-14", prison, LocalDate.of(2024, 3, 14))
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))
      createPrivateEstateBatch("PRV/2024-03-16", prison, LocalDate.of(2024, 3, 16))

      webTestClient.get()
        .uri("/private-estate-batches/?date__gte=2024-03-15")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-181 - Filter by prison")
    fun `should filter by prison`() {
      val prison1 = createPrivatePrison("PRV1")
      val prison2 = createPrivatePrison("PRV2")
      createPrivateEstateBatch("PRV1/2024-03-15", prison1, LocalDate.of(2024, 3, 15))
      createPrivateEstateBatch("PRV2/2024-03-15", prison2, LocalDate.of(2024, 3, 15))

      webTestClient.get()
        .uri("/private-estate-batches/?prison=PRV1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].prison").isEqualTo("PRV1")
    }
  }

  @Nested
  @DisplayName("GET /private-estate-batches/{ref}/ (CRD-183)")
  inner class GetPrivateEstateBatch {

    @Test
    @DisplayName("CRD-183 - GET /private-estate-batches/{prison}/{date}/ returns single batch")
    fun `should return single batch by ref`() {
      val prison = createPrivatePrison()
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))

      webTestClient.get()
        .uri("/private-estate-batches/PRV/2024-03-15/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.ref").isEqualTo("PRV/2024-03-15")
    }

    @Test
    @DisplayName("CRD-183 - GET non-existent batch returns 404")
    fun `should return 404 for non-existent batch ref`() {
      webTestClient.get()
        .uri("/private-estate-batches/UNKNOWN/2024-03-15/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("PATCH /private-estate-batches/{prison}/{date}/ (CRD-184 to CRD-186)")
  inner class PatchPrivateEstateBatch {

    @Test
    @DisplayName("CRD-184 - PATCH credits all credit_pending credits in batch")
    fun `should credit all credit_pending credits in batch`() {
      val prison = createPrivatePrison()
      val credit1 = createAndSaveCredit(prison = "PRV", resolution = CreditResolution.PENDING)
      val credit2 = createAndSaveCredit(prison = "PRV", resolution = CreditResolution.PENDING)
      createPrivateEstateBatch(
        "PRV/2024-03-15",
        prison,
        LocalDate.of(2024, 3, 15),
        credits = listOf(credit1, credit2),
        totalAmount = 2000,
      )

      webTestClient.patch()
        .uri("/private-estate-batches/PRV/2024-03-15/")
        .headers(setAuthorisation(username = "bankadmin"))
        .header("Content-Type", "application/json")
        .bodyValue("{}")
        .exchange()
        .expectStatus().isOk

      assertThat(creditRepository.findById(credit1.id!!).get().resolution).isEqualTo(CreditResolution.CREDITED)
      assertThat(creditRepository.findById(credit2.id!!).get().resolution).isEqualTo(CreditResolution.CREDITED)
    }

    @Test
    @DisplayName("CRD-185 - PATCH non-existent batch returns 404")
    fun `should return 404 for non-existent batch on PATCH`() {
      webTestClient.patch()
        .uri("/private-estate-batches/UNKNOWN/2024-03-15/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("{}")
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("GET /private-estate-batches/{prison}/{date}/credits/ (CRD-187 to CRD-189)")
  inner class GetPrivateEstateBatchCredits {

    @Test
    @DisplayName("CRD-187 - GET /private-estate-batches/{prison}/{date}/credits/ returns credits in batch")
    fun `should return credits for batch`() {
      val prison = createPrivatePrison()
      val credit1 = createAndSaveCredit(prison = "PRV")
      val credit2 = createAndSaveCredit(prison = "PRV")
      createPrivateEstateBatch(
        "PRV/2024-03-15",
        prison,
        LocalDate.of(2024, 3, 15),
        credits = listOf(credit1, credit2),
      )

      webTestClient.get()
        .uri("/private-estate-batches/PRV/2024-03-15/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-187 - GET credits for non-existent batch returns 404")
    fun `should return 404 for non-existent batch credits`() {
      webTestClient.get()
        .uri("/private-estate-batches/UNKNOWN/2024-03-15/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound
    }
  }
}

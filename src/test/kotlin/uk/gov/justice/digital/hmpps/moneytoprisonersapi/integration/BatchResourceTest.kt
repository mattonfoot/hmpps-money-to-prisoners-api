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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

class BatchResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var batchRepository: BatchRepository

  @Autowired
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @Autowired
  private lateinit var senderProfileRepository: SenderProfileRepository

  @Autowired
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @BeforeEach
  fun setUp() {
    privateEstateBatchRepository.deleteAll()
    senderProfileRepository.deleteAll()
    prisonerProfileRepository.deleteAll()
    batchRepository.deleteAll()
    creditRepository.deleteAll()
  }

  private fun createAndSaveCredit(): Credit {
    val credit = Credit(
      amount = 1000,
      prisonerNumber = "A1234BC",
      prisonerName = "John Smith",
      resolution = CreditResolution.PENDING,
    )
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  @Nested
  @DisplayName("POST /batches/ (CRD-170 to CRD-172)")
  inner class CreateBatch {

    @Test
    @DisplayName("CRD-170 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated POST`() {
      webTestClient.post()
        .uri("/batches/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": []}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-170 - POST /batches/ creates a batch with credit IDs")
    fun `should create batch with credit IDs`() {
      val credit = createAndSaveCredit()

      webTestClient.post()
        .uri("/batches/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.id").isNotEmpty
        .jsonPath("$.owner").isEqualTo("clerk1")
    }

    @Test
    @DisplayName("CRD-171 - Batch sets owner to requesting user")
    fun `should set batch owner to requesting user`() {
      val credit = createAndSaveCredit()

      webTestClient.post()
        .uri("/batches/")
        .headers(setAuthorisation(username = "manager1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.owner").isEqualTo("manager1")

      assertThat(batchRepository.count()).isEqualTo(1)
      assertThat(batchRepository.findAll()[0].owner).isEqualTo("manager1")
    }

    @Test
    @DisplayName("CRD-170 - Batch contains the specified credits")
    fun `should batch contain the specified credits`() {
      val credit1 = createAndSaveCredit()
      val credit2 = createAndSaveCredit()

      webTestClient.post()
        .uri("/batches/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit1.id}, ${credit2.id}]}""")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.credit_ids.length()").isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("GET /batches/ (CRD-172)")
  inner class ListBatches {

    @Test
    @DisplayName("CRD-172 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated GET`() {
      webTestClient.get()
        .uri("/batches/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-172 - GET /batches/ lists user's own batches")
    fun `should list user's own batches only`() {
      val credit1 = createAndSaveCredit()
      val credit2 = createAndSaveCredit()

      // Create batch for clerk1
      webTestClient.post()
        .uri("/batches/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit1.id}]}""")
        .exchange()
        .expectStatus().isCreated

      // Create batch for clerk2
      webTestClient.post()
        .uri("/batches/")
        .headers(setAuthorisation(username = "clerk2"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit2.id}]}""")
        .exchange()
        .expectStatus().isCreated

      // clerk1 should only see their own batches
      webTestClient.get()
        .uri("/batches/")
        .headers(setAuthorisation(username = "clerk1"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].owner").isEqualTo("clerk1")
    }

    @Test
    @DisplayName("CRD-172 - GET /batches/ returns empty list when user has no batches")
    fun `should return empty list when no batches`() {
      webTestClient.get()
        .uri("/batches/")
        .headers(setAuthorisation(username = "clerk1"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isArray
        .jsonPath("$.length()").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("DELETE /batches/{id}/ (CRD-173 to CRD-175)")
  inner class DeleteBatch {

    @Test
    @DisplayName("CRD-173 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated DELETE`() {
      webTestClient.delete()
        .uri("/batches/999/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-173 - DELETE /batches/{id}/ removes batch")
    fun `should remove batch`() {
      val credit = createAndSaveCredit()

      val batchId = webTestClient.post()
        .uri("/batches/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isCreated
        .returnResult(String::class.java)
        .responseBody.blockFirst()
        .let {
          // extract id from response
          Regex(""""id"\s*:\s*(\d+)""").find(it!!)?.groupValues?.get(1)?.toLong()
        }

      assertThat(batchId).isNotNull

      webTestClient.delete()
        .uri("/batches/$batchId/")
        .headers(setAuthorisation(username = "clerk1"))
        .exchange()
        .expectStatus().isNoContent

      assertThat(batchRepository.existsById(batchId!!)).isFalse
    }

    @Test
    @DisplayName("CRD-174 - DELETE does NOT modify credits")
    fun `should not modify credits when deleting batch`() {
      val credit = createAndSaveCredit()
      val originalResolution = credit.resolution

      val batchId = webTestClient.post()
        .uri("/batches/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isCreated
        .returnResult(String::class.java)
        .responseBody.blockFirst()
        .let {
          Regex(""""id"\s*:\s*(\d+)""").find(it!!)?.groupValues?.get(1)?.toLong()
        }

      webTestClient.delete()
        .uri("/batches/$batchId/")
        .headers(setAuthorisation(username = "clerk1"))
        .exchange()
        .expectStatus().isNoContent

      val updatedCredit = creditRepository.findById(credit.id!!).get()
      assertThat(updatedCredit.resolution).isEqualTo(originalResolution)
    }

    @Test
    @DisplayName("CRD-175 - DELETE non-existent batch returns 404")
    fun `should return 404 for non-existent batch`() {
      webTestClient.delete()
        .uri("/batches/99999/")
        .headers(setAuthorisation(username = "clerk1"))
        .exchange()
        .expectStatus().isNotFound
    }
  }
}

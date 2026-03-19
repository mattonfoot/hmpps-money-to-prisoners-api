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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import java.time.LocalDate

class ReconcileResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var logRepository: LogRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

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
    logRepository.deleteAll()
    creditRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun createPublicPrison(nomisId: String = "LEI"): Prison {
    val prison = Prison(nomisId = nomisId, name = "Leeds", region = "Yorkshire")
    prison.privateEstate = false
    return prisonRepository.save(prison)
  }

  private fun createPrivatePrison(nomisId: String = "PRV"): Prison {
    val prison = Prison(nomisId = nomisId, name = "Private Prison", region = "South")
    prison.privateEstate = true
    return prisonRepository.save(prison)
  }

  private fun createAndSaveCredit(prison: String? = "LEI", amount: Long = 1000): Credit {
    val credit = Credit(
      amount = amount,
      prisonerNumber = "A1234BC",
      prisonerName = "John Smith",
      prison = prison,
      resolution = CreditResolution.PENDING,
    )
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  @Nested
  @DisplayName("POST /credits/actions/reconcile/ (CRD-190 to CRD-195)")
  inner class ReconcileAction {

    @Test
    @DisplayName("CRD-194 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.post()
        .uri("/credits/actions/reconcile/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": []}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-195 - Empty credit_ids returns 204 no content")
    fun `should return 204 for empty credit_ids`() {
      webTestClient.post()
        .uri("/credits/actions/reconcile/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": []}""")
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("CRD-190 - Sets reconciled=true on each credit")
    fun `should set reconciled to true on each credit`() {
      createPublicPrison("LEI")
      val credit1 = createAndSaveCredit(prison = "LEI", amount = 1000)
      val credit2 = createAndSaveCredit(prison = "LEI", amount = 2000)

      webTestClient.post()
        .uri("/credits/actions/reconcile/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit1.id}, ${credit2.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(creditRepository.findById(credit1.id!!).get().reconciled).isTrue()
      assertThat(creditRepository.findById(credit2.id!!).get().reconciled).isTrue()
    }

    @Test
    @DisplayName("CRD-191 - Creates RECONCILED log for each credit")
    fun `should create RECONCILED log for each credit`() {
      createPublicPrison("LEI")
      val credit = createAndSaveCredit(prison = "LEI")

      webTestClient.post()
        .uri("/credits/actions/reconcile/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val logs = logRepository.findByCreditId(credit.id!!)
      assertThat(logs.any { it.action == LogAction.RECONCILED && it.userId == "clerk1" }).isTrue()
    }

    @Test
    @DisplayName("CRD-192 - Creates PrivateEstateBatch for credit in private prison")
    fun `should create PrivateEstateBatch for private prison credit`() {
      createPrivatePrison("PRV")
      val credit = createAndSaveCredit(prison = "PRV", amount = 1500)

      webTestClient.post()
        .uri("/credits/actions/reconcile/")
        .headers(setAuthorisation(username = "bankadmin"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val today = LocalDate.now()
      val batch = privateEstateBatchRepository.findById("PRV/$today")
      assertThat(batch).isPresent
      assertThat(batch.get().prison).isEqualTo("PRV")
      assertThat(batch.get().date).isEqualTo(today)
    }

    @Test
    @DisplayName("CRD-193 - PrivateEstateBatch has correct ref and total_amount")
    fun `should set correct ref and total_amount on PrivateEstateBatch`() {
      createPrivatePrison("PRV")
      val credit1 = createAndSaveCredit(prison = "PRV", amount = 1000)
      val credit2 = createAndSaveCredit(prison = "PRV", amount = 2500)

      webTestClient.post()
        .uri("/credits/actions/reconcile/")
        .headers(setAuthorisation(username = "bankadmin"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit1.id}, ${credit2.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val today = LocalDate.now()
      val batch = privateEstateBatchRepository.findById("PRV/$today").get()
      assertThat(batch.ref).isEqualTo("PRV/$today")
      assertThat(batch.totalAmount).isEqualTo(3500L)
    }

    @Test
    @DisplayName("CRD-192 - No PrivateEstateBatch created for public prison credit")
    fun `should not create PrivateEstateBatch for public prison credit`() {
      createPublicPrison("LEI")
      val credit = createAndSaveCredit(prison = "LEI", amount = 1000)

      webTestClient.post()
        .uri("/credits/actions/reconcile/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(privateEstateBatchRepository.count()).isZero()
    }
  }
}

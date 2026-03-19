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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BillingAddressRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.TransactionRepository
import java.util.UUID

class AttachProfilesResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var transactionRepository: TransactionRepository

  @Autowired
  private lateinit var paymentRepository: PaymentRepository

  @Autowired
  private lateinit var billingAddressRepository: BillingAddressRepository

  @Autowired
  private lateinit var senderProfileRepository: SenderProfileRepository

  @Autowired
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @Autowired
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @BeforeEach
  fun setUp() {
    privateEstateBatchRepository.deleteAll()
    senderProfileRepository.deleteAll()
    prisonerProfileRepository.deleteAll()
    paymentRepository.deleteAll()
    transactionRepository.deleteAll()
    creditRepository.deleteAll()
    billingAddressRepository.deleteAll()
  }

  private fun createAndSaveCreditWithTransaction(
    prisonerNumber: String? = "A1234BC",
    senderSortCode: String? = "112233",
    senderAccountNumber: String? = "12345678",
  ): Credit {
    val credit = Credit(
      amount = 1000,
      prisonerNumber = prisonerNumber,
      prisonerName = "John Smith",
      resolution = CreditResolution.PENDING,
    )
    credit.source = CreditSource.BANK_TRANSFER
    val savedCredit = creditRepository.save(credit)

    val transaction = Transaction(amount = 1000)
    transaction.senderSortCode = senderSortCode
    transaction.senderAccountNumber = senderAccountNumber
    transaction.senderName = "Alice Sender"
    transaction.credit = savedCredit
    transactionRepository.save(transaction)

    return savedCredit
  }

  private fun createAndSaveCreditWithPayment(
    prisonerNumber: String? = "A1234BC",
    cardNumberFirstDigits: String? = "411111",
    cardNumberLastDigits: String? = "1234",
    cardExpiryDate: String? = "1225",
  ): Credit {
    val credit = Credit(
      amount = 1000,
      prisonerNumber = prisonerNumber,
      prisonerName = "John Smith",
      resolution = CreditResolution.PENDING,
    )
    credit.source = CreditSource.ONLINE
    val savedCredit = creditRepository.save(credit)

    val payment = Payment(uuid = UUID.randomUUID())
    payment.cardNumberFirstDigits = cardNumberFirstDigits
    payment.cardNumberLastDigits = cardNumberLastDigits
    payment.cardExpiryDate = cardExpiryDate
    payment.credit = savedCredit
    paymentRepository.save(payment)

    return savedCredit
  }

  @Nested
  @DisplayName("POST /credits/actions/attach-profiles/ (CRD-210 to CRD-215)")
  inner class AttachProfiles {

    @Test
    @DisplayName("CRD-214 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.post()
        .uri("/credits/actions/attach-profiles/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": []}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-215 - Empty credit_ids returns 204 no content")
    fun `should return 204 for empty credit_ids`() {
      webTestClient.post()
        .uri("/credits/actions/attach-profiles/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": []}""")
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("CRD-210 - Creates SenderProfile for bank transfer credit")
    fun `should create SenderProfile for bank transfer credit`() {
      val credit = createAndSaveCreditWithTransaction(
        prisonerNumber = "A1234BC",
        senderSortCode = "112233",
        senderAccountNumber = "12345678",
      )

      webTestClient.post()
        .uri("/credits/actions/attach-profiles/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(senderProfileRepository.count()).isEqualTo(1L)
    }

    @Test
    @DisplayName("CRD-211 - Reuses existing SenderProfile for same bank account")
    fun `should reuse existing SenderProfile for same bank account`() {
      val credit1 = createAndSaveCreditWithTransaction(senderSortCode = "112233", senderAccountNumber = "12345678")
      val credit2 = createAndSaveCreditWithTransaction(senderSortCode = "112233", senderAccountNumber = "12345678")

      webTestClient.post()
        .uri("/credits/actions/attach-profiles/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit1.id}, ${credit2.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      // Only one profile created; both credits attached to it
      assertThat(senderProfileRepository.count()).isEqualTo(1L)
    }

    @Test
    @DisplayName("CRD-210 - Creates SenderProfile for online payment credit")
    fun `should create SenderProfile for online payment credit`() {
      val credit = createAndSaveCreditWithPayment(
        cardNumberFirstDigits = "411111",
        cardNumberLastDigits = "1234",
        cardExpiryDate = "1225",
      )

      webTestClient.post()
        .uri("/credits/actions/attach-profiles/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(senderProfileRepository.count()).isEqualTo(1L)
    }

    @Test
    @DisplayName("CRD-212 - Creates PrisonerProfile for credit")
    fun `should create PrisonerProfile for credit`() {
      val credit = createAndSaveCreditWithTransaction(prisonerNumber = "A1234BC")

      webTestClient.post()
        .uri("/credits/actions/attach-profiles/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(prisonerProfileRepository.count()).isEqualTo(1L)
    }

    @Test
    @DisplayName("CRD-213 - Reuses existing PrisonerProfile for same prisoner number")
    fun `should reuse existing PrisonerProfile for same prisoner number`() {
      val credit1 = createAndSaveCreditWithTransaction(prisonerNumber = "A1234BC")
      val credit2 = createAndSaveCreditWithTransaction(prisonerNumber = "A1234BC")

      webTestClient.post()
        .uri("/credits/actions/attach-profiles/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit1.id}, ${credit2.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      // Only one profile created for the same prisoner number
      assertThat(prisonerProfileRepository.count()).isEqualTo(1L)
    }

    @Test
    @DisplayName("CRD-215 - Credit with no prisoner number creates no PrisonerProfile")
    fun `should not create PrisonerProfile when prisoner number is null`() {
      val credit = createAndSaveCreditWithTransaction(prisonerNumber = null)

      webTestClient.post()
        .uri("/credits/actions/attach-profiles/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(prisonerProfileRepository.count()).isZero()
    }
  }
}

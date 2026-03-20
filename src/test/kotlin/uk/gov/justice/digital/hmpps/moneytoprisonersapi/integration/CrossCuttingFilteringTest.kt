package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.BillingAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BillingAddressRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.TransactionRepository
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("14.3 Filtering")
class CrossCuttingFilteringTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var disbursementRepository: DisbursementRepository

  @Autowired
  private lateinit var transactionRepository: TransactionRepository

  @Autowired
  private lateinit var paymentRepository: PaymentRepository

  @Autowired
  private lateinit var billingAddressRepository: BillingAddressRepository

  @BeforeEach
  fun setUp() {
    paymentRepository.deleteAll()
    transactionRepository.deleteAll()
    disbursementRepository.deleteAll()
    creditRepository.deleteAll()
    prisonRepository.deleteAll()
    billingAddressRepository.deleteAll()
  }

  private fun savePrison(nomisId: String) = prisonRepository.save(Prison(nomisId = nomisId, name = nomisId, region = ""))

  private fun saveCredit(
    prison: String? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
    prisonerName: String? = "John Smith",
    prisonerNumber: String? = "A1234BC",
    receivedAt: LocalDateTime? = null,
    amount: Long = 1000L,
  ): Credit {
    if (prison != null && !prisonRepository.existsById(prison)) savePrison(prison)
    val credit = Credit(
      amount = amount,
      resolution = resolution,
      prisonerName = prisonerName,
      prisonerNumber = prisonerNumber,
      prison = prison,
      receivedAt = receivedAt,
    )
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  private fun saveTransaction(credit: Credit, senderName: String = "Alice Sender"): Transaction {
    val transaction = Transaction(amount = credit.amount, senderName = senderName)
    transaction.credit = credit
    credit.source = CreditSource.BANK_TRANSFER
    creditRepository.save(credit)
    return transactionRepository.save(transaction)
  }

  private fun savePaymentWithBillingAddress(credit: Credit, postcode: String): Payment {
    val address = billingAddressRepository.save(BillingAddress(postcode = postcode))
    val payment = Payment(uuid = UUID.randomUUID(), amount = credit.amount)
    payment.credit = credit
    payment.billingAddress = address
    credit.source = CreditSource.ONLINE
    creditRepository.save(credit)
    return paymentRepository.save(payment)
  }

  private fun saveDisbursement(prison: String = "LEI", postcode: String? = null): Disbursement {
    if (!prisonRepository.existsById(prison)) savePrison(prison)
    return disbursementRepository.save(
      Disbursement(
        amount = 500L,
        prison = prison,
        resolution = DisbursementResolution.PENDING,
        method = DisbursementMethod.BANK_TRANSFER,
        postcode = postcode,
      ),
    )
  }

  @Nested
  @DisplayName("XCT-020 Query parameter filtering")
  inner class QueryParameterFiltering {

    @Test
    @DisplayName("XCT-020 credits filtered by resolution returns only matching credits")
    fun `credits can be filtered by resolution query parameter`() {
      saveCredit(resolution = CreditResolution.PENDING)
      saveCredit(resolution = CreditResolution.CREDITED)
      saveCredit(resolution = CreditResolution.REFUNDED)

      webTestClient.get()
        .uri("/credits/?resolution=CREDITED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].resolution").isEqualTo("CREDITED")
    }

    @Test
    @DisplayName("XCT-020 credits filtered by prisoner_number returns only exact matches")
    fun `credits can be filtered by exact prisoner number`() {
      saveCredit(prisonerNumber = "A1234BC")
      saveCredit(prisonerNumber = "B5678DE")
      saveCredit(prisonerNumber = "A1234BC")

      webTestClient.get()
        .uri("/credits/?prisoner_number=A1234BC")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("XCT-020 disbursements can be filtered by prison query parameter")
    fun `disbursements can be filtered by prison`() {
      saveDisbursement(prison = "LEI")
      saveDisbursement(prison = "MDI")

      webTestClient.get()
        .uri("/disbursements/?prison=LEI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("XCT-021 Multiple value filter supports comma-separated and repeated values")
  inner class MultipleValueFilter {

    @Test
    @DisplayName("XCT-021 credits filtered by multiple prisons using repeated parameter")
    fun `credits can be filtered by multiple prison values using repeated query param`() {
      saveCredit(prison = "LEI")
      saveCredit(prison = "MDI")
      saveCredit(prison = "BWI")

      webTestClient.get()
        .uri("/credits/?prison=LEI&prison=MDI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("XCT-021 disbursements filtered by multiple resolutions using repeated parameter")
    fun `disbursements can be filtered by multiple resolution values`() {
      disbursementRepository.save(
        Disbursement(amount = 100L, prison = "LEI", resolution = DisbursementResolution.PENDING, method = DisbursementMethod.BANK_TRANSFER),
      )
      disbursementRepository.save(
        Disbursement(amount = 200L, prison = "LEI", resolution = DisbursementResolution.SENT, method = DisbursementMethod.BANK_TRANSFER),
      )
      disbursementRepository.save(
        Disbursement(amount = 300L, prison = "LEI", resolution = DisbursementResolution.REJECTED, method = DisbursementMethod.BANK_TRANSFER),
      )

      webTestClient.get()
        .uri("/disbursements/?resolution=PENDING&resolution=SENT")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("XCT-022 PostcodeFilter normalizes postcodes")
  inner class PostcodeFilter {

    @Test
    @DisplayName("XCT-022 postcode filter matches ignoring spaces in stored postcode")
    fun `postcode filter matches with spaces removed`() {
      val credit1 = saveCredit()
      savePaymentWithBillingAddress(credit1, postcode = "SW1A 1AA")
      val credit2 = saveCredit()
      savePaymentWithBillingAddress(credit2, postcode = "EC2A 3QR")

      webTestClient.get()
        .uri("/credits/?sender_postcode=SW1A1AA")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("XCT-022 postcode filter is case-insensitive")
    fun `postcode filter matches case-insensitively`() {
      val credit = saveCredit()
      savePaymentWithBillingAddress(credit, postcode = "SW1A 1AA")

      webTestClient.get()
        .uri("/credits/?sender_postcode=sw1a1aa")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("XCT-022 disbursement postcode filter normalizes spaces and case")
    fun `disbursement postcode filter normalizes stored postcode`() {
      saveDisbursement(postcode = "SW1A 2AA")
      saveDisbursement(postcode = "EC1A 1BB")

      webTestClient.get()
        .uri("/disbursements/?postcode=SW1A2AA")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("XCT-022 postcode filter matches stored value that has spaces when query has spaces too")
    fun `postcode filter matches when both stored and query have spaces`() {
      val credit = saveCredit()
      savePaymentWithBillingAddress(credit, postcode = "SW1A 1AA")

      webTestClient.get()
        .uri("/credits/?sender_postcode=SW1A 1AA")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("XCT-023 SplitTextInMultipleFieldsFilter for search")
  inner class SplitTextSearch {

    @Test
    @DisplayName("XCT-023 search splits space-separated terms and requires ALL to match (AND logic)")
    fun `search requires all words to match across fields`() {
      val credit1 = saveCredit(prisonerName = "John Smith")
      saveTransaction(credit1, senderName = "Alice Sender")
      val credit2 = saveCredit(prisonerName = "Jane Doe")
      saveTransaction(credit2, senderName = "Bob Sender")

      webTestClient.get()
        .uri("/credits/?search=John Smith")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_name").isEqualTo("John Smith")
    }

    @Test
    @DisplayName("XCT-023 search does not match when only one of multiple terms matches")
    fun `search with multiple words requires all to match`() {
      val credit = saveCredit(prisonerName = "John")
      saveTransaction(credit, senderName = "Alice")

      webTestClient.get()
        .uri("/credits/?search=John Smith") // "Smith" does not match
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("XCT-023 search matches across different fields (prisoner name and sender name)")
    fun `search can match across prisoner name and sender name fields`() {
      val credit = saveCredit(prisonerName = "John Prisoner")
      saveTransaction(credit, senderName = "Alice Sender")

      // "John" in prisoner_name, "Alice" in sender_name — both terms must match across fields
      webTestClient.get()
        .uri("/credits/?search=John Alice")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("XCT-024 Date range filters use lt/gte convention")
  inner class DateRangeFilters {

    private val baseTime = LocalDateTime.of(2024, 6, 15, 12, 0, 0)

    @Test
    @DisplayName("XCT-024 received_at__gte is inclusive — includes credits at exactly that datetime")
    fun `received_at__gte includes credits at exactly that time (inclusive lower bound)`() {
      saveCredit(receivedAt = baseTime) // exactly at boundary — should be included
      saveCredit(receivedAt = baseTime.minusSeconds(1)) // before — should be excluded

      webTestClient.get()
        .uri("/credits/?received_at__gte=2024-06-15T12:00:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("XCT-024 received_at__lt is exclusive — excludes credits at exactly that datetime")
    fun `received_at__lt excludes credits at exactly that time (exclusive upper bound)`() {
      saveCredit(receivedAt = baseTime.minusSeconds(1)) // before — should be included
      saveCredit(receivedAt = baseTime) // exactly at boundary — should be excluded

      webTestClient.get()
        .uri("/credits/?received_at__lt=2024-06-15T12:00:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("XCT-024 combining received_at__gte and received_at__lt creates a half-open interval")
    fun `combined date range filter creates half-open interval`() {
      saveCredit(receivedAt = baseTime.minusDays(1)) // before range
      saveCredit(receivedAt = baseTime) // at start (included)
      saveCredit(receivedAt = baseTime.plusHours(1)) // inside range
      saveCredit(receivedAt = baseTime.plusDays(1)) // at end (excluded)

      webTestClient.get()
        .uri("/credits/?received_at__gte=2024-06-15T12:00:00&received_at__lt=2024-06-16T12:00:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("XCT-024 credits with null received_at are excluded by date range filters")
    fun `credits with null received_at are excluded from date range results`() {
      saveCredit(receivedAt = null) // no timestamp
      saveCredit(receivedAt = baseTime) // has timestamp

      webTestClient.get()
        .uri("/credits/?received_at__gte=2024-06-15T12:00:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }
  }

  @Test
  @DisplayName("XCT-020 no filter returns all records")
  fun `no filter params returns all records`() {
    val results = listOf(
      assertThat(creditRepository.count()).isEqualTo(0),
    )
    saveCredit()
    saveCredit()
    saveCredit()

    webTestClient.get()
      .uri("/credits/")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.count").isEqualTo(3)
  }
}

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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.TransactionRepository
import java.time.LocalDateTime

class TransactionResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var transactionRepository: TransactionRepository

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @BeforeEach
  fun setUp() {
    privateEstateBatchRepository.deleteAll()
    transactionRepository.deleteAll()
    creditRepository.deleteAll()
  }

  // -------------------------------------------------------------------------
  // TXN-020 to TXN-027: POST /transactions/ and GET /transactions/
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /transactions/ (TXN-020 to TXN-022)")
  inner class CreateTransactions {

    @Test
    @DisplayName("TXN-022 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated POST`() {
      webTestClient.post()
        .uri("/transactions/")
        .header("Content-Type", "application/json")
        .bodyValue("""[{"amount": 1000, "category": "credit", "source": "bank_transfer"}]""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("TXN-022 - Requires ROLE_BANK_ADMIN, returns 403 for missing role")
    fun `should return 403 without ROLE_BANK_ADMIN`() {
      webTestClient.post()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_OTHER")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"amount": 1000, "category": "credit", "source": "bank_transfer"}]""")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("TXN-020 - POST /transactions/ bulk creates transactions and returns 201")
    fun `should bulk create transactions and return 201`() {
      webTestClient.post()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """[
            {
              "amount": 1000,
              "category": "credit",
              "source": "bank_transfer",
              "sender_sort_code": "112233",
              "sender_account_number": "12345678",
              "sender_name": "Alice Sender",
              "ref_code": "REF001"
            },
            {
              "amount": 2000,
              "category": "credit",
              "source": "bank_transfer",
              "sender_sort_code": "445566",
              "sender_account_number": "87654321",
              "sender_name": "Bob Sender",
              "ref_code": "REF002"
            }
          ]""",
        )
        .exchange()
        .expectStatus().isCreated

      val transactions = transactionRepository.findAll()
      assertThat(transactions).hasSize(2)
    }

    @Test
    @DisplayName("TXN-021 - Auto-creates Credit for bank_transfer credit category transactions")
    fun `should auto-create credit for bank_transfer credit transactions`() {
      webTestClient.post()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """[{
            "amount": 5000,
            "category": "credit",
            "source": "bank_transfer",
            "sender_sort_code": "112233",
            "sender_account_number": "12345678",
            "sender_name": "Alice Sender",
            "received_at": "2024-01-15T10:30:00"
          }]""",
        )
        .exchange()
        .expectStatus().isCreated

      val transactions = transactionRepository.findAll()
      assertThat(transactions).hasSize(1)
      val txn = transactions[0]
      assertThat(txn.credit).isNotNull
      assertThat(txn.credit!!.amount).isEqualTo(5000L)
      assertThat(txn.credit!!.source).isEqualTo(CreditSource.BANK_TRANSFER)
      assertThat(txn.credit!!.resolution).isEqualTo(CreditResolution.PENDING)
    }

    @Test
    @DisplayName("TXN-021 - Does not auto-create Credit for debit transactions")
    fun `should not auto-create credit for debit transactions`() {
      webTestClient.post()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """[{
            "amount": 5000,
            "category": "debit",
            "source": "bank_transfer",
            "sender_sort_code": "112233",
            "sender_account_number": "12345678",
            "sender_name": "Alice Sender"
          }]""",
        )
        .exchange()
        .expectStatus().isCreated

      val transactions = transactionRepository.findAll()
      assertThat(transactions).hasSize(1)
      assertThat(transactions[0].credit).isNull()
    }
  }

  @Nested
  @DisplayName("GET /transactions/ (TXN-025 to TXN-027)")
  inner class ListTransactions {

    @Test
    @DisplayName("TXN-022 - GET requires ROLE_BANK_ADMIN, returns 401 unauthenticated")
    fun `should return 401 for unauthenticated GET`() {
      webTestClient.get()
        .uri("/transactions/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("TXN-022 - GET requires ROLE_BANK_ADMIN, returns 403 for missing role")
    fun `should return 403 without ROLE_BANK_ADMIN on GET`() {
      webTestClient.get()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_OTHER")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("TXN-025 - Filter by status=creditable")
    fun `should filter transactions by creditable status`() {
      // Ensure prison exists to satisfy FK constraint
      if (!prisonRepository.existsById("LEI")) {
        prisonRepository.save(Prison(nomisId = "LEI", name = "Leeds"))
      }

      // Create credit with prison assigned = creditable
      val credit1 = Credit(amount = 1000L, prison = "LEI", blocked = false, resolution = CreditResolution.PENDING)
      credit1.source = CreditSource.BANK_TRANSFER
      val savedCredit1 = creditRepository.save(credit1)
      val txn1 = Transaction(
        amount = 1000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER,
        credit = savedCredit1,
      )
      transactionRepository.save(txn1)

      // Create credit with no prison = refundable (not creditable)
      val credit2 = Credit(amount = 2000L, prison = null, blocked = false, resolution = CreditResolution.PENDING)
      credit2.source = CreditSource.BANK_TRANSFER
      val savedCredit2 = creditRepository.save(credit2)
      val txn2 = Transaction(
        amount = 2000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER,
        credit = savedCredit2,
      )
      transactionRepository.save(txn2)

      webTestClient.get()
        .uri("/transactions/?status=creditable")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("creditable")
    }

    @Test
    @DisplayName("TXN-026 - Filter by received_at__gte and received_at__lt")
    fun `should filter transactions by received_at date range`() {
      val credit1 = Credit(amount = 1000L, resolution = CreditResolution.PENDING, receivedAt = LocalDateTime.of(2024, 1, 15, 10, 0, 0))
      credit1.source = CreditSource.BANK_TRANSFER
      val savedCredit1 = creditRepository.save(credit1)
      val txn1 = Transaction(
        amount = 1000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER,
        receivedAt = LocalDateTime.of(2024, 1, 15, 10, 0, 0), credit = savedCredit1,
      )
      transactionRepository.save(txn1)

      val credit2 = Credit(amount = 2000L, resolution = CreditResolution.PENDING, receivedAt = LocalDateTime.of(2024, 3, 10, 9, 0, 0))
      credit2.source = CreditSource.BANK_TRANSFER
      val savedCredit2 = creditRepository.save(credit2)
      val txn2 = Transaction(
        amount = 2000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER,
        receivedAt = LocalDateTime.of(2024, 3, 10, 9, 0, 0), credit = savedCredit2,
      )
      transactionRepository.save(txn2)

      webTestClient.get()
        .uri("/transactions/?received_at__gte=2024-01-01T00:00:00&received_at__lt=2024-02-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(1000)
    }

    @Test
    @DisplayName("TXN-027 - Filter by multiple IDs (pk parameter)")
    fun `should filter transactions by multiple IDs`() {
      val txn1 = Transaction(amount = 1000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER)
      val savedTxn1 = transactionRepository.save(txn1)

      val txn2 = Transaction(amount = 2000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER)
      transactionRepository.save(txn2)

      val txn3 = Transaction(amount = 3000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER)
      val savedTxn3 = transactionRepository.save(txn3)

      webTestClient.get()
        .uri("/transactions/?pk=${savedTxn1.id}&pk=${savedTxn3.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("GET /transactions/ returns paginated list")
    fun `should return paginated list of all transactions`() {
      transactionRepository.save(Transaction(amount = 1000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER))
      transactionRepository.save(Transaction(amount = 2000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER))

      webTestClient.get()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results.length()").isEqualTo(2)
    }
  }

  // -------------------------------------------------------------------------
  // TXN-023 to TXN-024: PATCH /transactions/ — bulk refund
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("PATCH /transactions/ (TXN-023 to TXN-024)")
  inner class RefundTransactions {

    @Test
    @DisplayName("TXN-022 - PATCH requires ROLE_BANK_ADMIN, returns 401 unauthenticated")
    fun `should return 401 for unauthenticated PATCH`() {
      webTestClient.patch()
        .uri("/transactions/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"transaction_ids": [1]}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("TXN-022 - PATCH requires ROLE_BANK_ADMIN, returns 403 for missing role")
    fun `should return 403 without ROLE_BANK_ADMIN on PATCH`() {
      webTestClient.patch()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_OTHER")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"transaction_ids": [1]}""")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("TXN-023 - PATCH /transactions/ bulk refunds eligible transactions and returns 204")
    fun `should refund transactions in refundable state and return 204`() {
      // Create refundable transaction: credit exists, sender info complete, no prison
      val credit = Credit(amount = 1000L, prison = null, blocked = false, resolution = CreditResolution.PENDING)
      credit.source = CreditSource.BANK_TRANSFER
      val savedCredit = creditRepository.save(credit)

      val txn = Transaction(
        amount = 1000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER,
        incompleteSenderInfo = false, credit = savedCredit,
      )
      val savedTxn = transactionRepository.save(txn)

      webTestClient.patch()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"transaction_ids": [${savedTxn.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val updatedCredit = creditRepository.findById(savedCredit.id!!).get()
      assertThat(updatedCredit.resolution).isEqualTo(CreditResolution.REFUNDED)
    }

    @Test
    @DisplayName("TXN-024 - Returns 409 when transaction credit is in invalid state for refund")
    fun `should return 409 for non-refundable transactions`() {
      // Create a transaction with no credit (anonymous) — not refundable
      val txn = Transaction(
        amount = 1000L, category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER,
        incompleteSenderInfo = true, credit = null,
      )
      val savedTxn = transactionRepository.save(txn)

      webTestClient.patch()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"transaction_ids": [${savedTxn.id}]}""")
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.conflict_ids[0]").isEqualTo(savedTxn.id!!)
    }

    @Test
    @DisplayName("TXN-023 - Returns 400 for empty transaction_ids list")
    fun `should return 400 for empty transaction_ids`() {
      webTestClient.patch()
        .uri("/transactions/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"transaction_ids": []}""")
        .exchange()
        .expectStatus().isBadRequest
    }
  }
}

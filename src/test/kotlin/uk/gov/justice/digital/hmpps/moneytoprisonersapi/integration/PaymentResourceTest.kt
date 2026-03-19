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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BillingAddressRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import java.time.LocalDate
import java.time.LocalDateTime

class PaymentResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var paymentRepository: PaymentRepository

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var billingAddressRepository: BillingAddressRepository

  @Autowired
  private lateinit var batchRepository: BatchRepository

  @Autowired
  private lateinit var paymentBatchRepository: PaymentBatchRepository

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
    paymentBatchRepository.deleteAll()
    batchRepository.deleteAll()
    paymentRepository.deleteAll()
    creditRepository.deleteAll()
    billingAddressRepository.deleteAll()
  }

  // -------------------------------------------------------------------------
  // PAY-020 to PAY-027: POST /payments/
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /payments/ (PAY-020 to PAY-027)")
  inner class CreatePayment {

    @Test
    @DisplayName("PAY-027 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated POST`() {
      webTestClient.post()
        .uri("/payments/")
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"prisoner_number": "A1234BC", "prisoner_dob": "1990-01-01", "amount": 1000}""",
        )
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PAY-026 - Requires ROLE_SEND_MONEY, returns 403 for missing role")
    fun `should return 403 without ROLE_SEND_MONEY`() {
      webTestClient.post()
        .uri("/payments/")
        .headers(setAuthorisation(roles = listOf("ROLE_OTHER")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"prisoner_number": "A1234BC", "prisoner_dob": "1990-01-01", "amount": 1000}""",
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PAY-020 - POST /payments/ creates payment with status=pending and returns 201 with uuid")
    fun `should create payment and return 201 with uuid`() {
      webTestClient.post()
        .uri("/payments/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"prisoner_number": "A1234BC", "prisoner_dob": "1990-01-01", "amount": 1000}""",
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.uuid").isNotEmpty
        .jsonPath("$.status").isEqualTo("pending")
        .jsonPath("$.amount").isEqualTo(1000)
    }

    @Test
    @DisplayName("PAY-022 - Missing prisoner_number returns 400")
    fun `should return 400 when prisoner_number is missing`() {
      webTestClient.post()
        .uri("/payments/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"prisoner_dob": "1990-01-01", "amount": 1000}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("PAY-023 - Missing prisoner_dob returns 400")
    fun `should return 400 when prisoner_dob is missing`() {
      webTestClient.post()
        .uri("/payments/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"prisoner_number": "A1234BC", "amount": 1000}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("PAY-024 - Missing amount returns 400")
    fun `should return 400 when amount is missing`() {
      webTestClient.post()
        .uri("/payments/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"prisoner_number": "A1234BC", "prisoner_dob": "1990-01-01"}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("PAY-021 - Credit is created with INITIAL resolution and ONLINE source")
    fun `should create linked credit with INITIAL resolution and ONLINE source`() {
      webTestClient.post()
        .uri("/payments/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"prisoner_number": "A1234BC", "prisoner_dob": "1990-01-15", "amount": 5000}""",
        )
        .exchange()
        .expectStatus().isCreated

      val payments = paymentRepository.findAll()
      assertThat(payments).hasSize(1)
      val payment = payments[0]
      assertThat(payment.status).isEqualTo("pending")
      assertThat(payment.credit).isNotNull()

      val credit = payment.credit!!
      assertThat(credit.resolution).isEqualTo(CreditResolution.INITIAL)
      assertThat(credit.source).isEqualTo(CreditSource.ONLINE)
      assertThat(credit.prisonerNumber).isEqualTo("A1234BC")
      assertThat(credit.prisonerDob).isEqualTo(LocalDate.of(1990, 1, 15))
      assertThat(credit.amount).isEqualTo(5000L)
    }
  }

  // -------------------------------------------------------------------------
  // PAY-030 to PAY-038: PATCH /payments/{uuid}/
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("PATCH /payments/{uuid}/ (PAY-030 to PAY-038)")
  inner class UpdatePayment {

    private fun createPendingPayment(): Payment {
      val credit = Credit(
        amount = 1000L,
        prisonerNumber = "A1234BC",
        prisonerDob = LocalDate.of(1990, 1, 1),
        resolution = CreditResolution.INITIAL,
      )
      credit.source = CreditSource.ONLINE
      val savedCredit = creditRepository.save(credit)
      val payment = Payment(amount = 1000L, status = "pending", credit = savedCredit)
      return paymentRepository.save(payment)
    }

    @Test
    @DisplayName("PAY-030 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated PATCH`() {
      webTestClient.patch()
        .uri("/payments/00000000-0000-0000-0000-000000000000/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "taken"}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PAY-030 - Returns 404 for non-existent payment")
    fun `should return 404 for non-existent payment UUID`() {
      webTestClient.patch()
        .uri("/payments/00000000-0000-0000-0000-000000000001/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "taken"}""")
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    @DisplayName("PAY-031 - Non-pending payment update returns 409")
    fun `should return 409 when updating non-pending payment`() {
      // Create a taken payment
      val credit = Credit(
        amount = 1000L,
        prisonerNumber = "A1234BC",
        resolution = CreditResolution.PENDING,
      )
      credit.source = CreditSource.ONLINE
      val savedCredit = creditRepository.save(credit)
      val payment = Payment(amount = 1000L, status = "taken", credit = savedCredit)
      val savedPayment = paymentRepository.save(payment)

      webTestClient.patch()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "failed"}""")
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.errors[0]").value<String> { msg ->
          assertThat(msg).contains("taken")
        }
    }

    @Test
    @DisplayName("PAY-032 - Transition to taken sets credit to PENDING")
    fun `should transition credit to PENDING when payment taken`() {
      val savedPayment = createPendingPayment()

      webTestClient.patch()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "taken"}""")
        .exchange()
        .expectStatus().isOk

      val updatedCredit = creditRepository.findById(savedPayment.credit!!.id!!).get()
      assertThat(updatedCredit.resolution).isEqualTo(CreditResolution.PENDING)
      assertThat(updatedCredit.receivedAt).isNotNull()
    }

    @Test
    @DisplayName("PAY-033 - Transition to failed leaves credit as INITIAL")
    fun `should leave credit as INITIAL when payment failed`() {
      val savedPayment = createPendingPayment()
      val creditId = savedPayment.credit!!.id!!

      webTestClient.patch()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "failed"}""")
        .exchange()
        .expectStatus().isOk

      val updatedCredit = creditRepository.findById(creditId).get()
      assertThat(updatedCredit.resolution).isEqualTo(CreditResolution.INITIAL)
    }

    @Test
    @DisplayName("PAY-034 - Transition to rejected sets credit to FAILED")
    fun `should set credit to FAILED when payment rejected`() {
      val savedPayment = createPendingPayment()
      val creditId = savedPayment.credit!!.id!!

      webTestClient.patch()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "rejected"}""")
        .exchange()
        .expectStatus().isOk

      val updatedCredit = creditRepository.findById(creditId).get()
      assertThat(updatedCredit.resolution).isEqualTo(CreditResolution.FAILED)
    }

    @Test
    @DisplayName("PAY-035 - Transition to expired sets credit to FAILED")
    fun `should set credit to FAILED when payment expired`() {
      val savedPayment = createPendingPayment()
      val creditId = savedPayment.credit!!.id!!

      webTestClient.patch()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "expired"}""")
        .exchange()
        .expectStatus().isOk

      val updatedCredit = creditRepository.findById(creditId).get()
      assertThat(updatedCredit.resolution).isEqualTo(CreditResolution.FAILED)
    }

    @Test
    @DisplayName("PAY-036 - Card details updated on PATCH")
    fun `should update card details`() {
      val savedPayment = createPendingPayment()

      webTestClient.patch()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{
            "cardholder_name": "John Doe",
            "card_number_first_digits": "411111",
            "card_number_last_digits": "1234",
            "card_expiry_date": "12/25",
            "card_brand": "Visa"
          }""",
        )
        .exchange()
        .expectStatus().isOk

      val updated = paymentRepository.findById(savedPayment.uuid).get()
      assertThat(updated.cardholderName).isEqualTo("John Doe")
      assertThat(updated.cardNumberFirstDigits).isEqualTo("411111")
      assertThat(updated.cardNumberLastDigits).isEqualTo("1234")
    }

    @Test
    @DisplayName("PAY-037 - Billing address created and linked on first PATCH")
    fun `should create billing address on first PATCH`() {
      val savedPayment = createPendingPayment()

      webTestClient.patch()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{
            "billing_address": {
              "line1": "10 Downing Street",
              "city": "London",
              "postcode": "SW1A 2AA",
              "country": "GB"
            }
          }""",
        )
        .exchange()
        .expectStatus().isOk

      val updated = paymentRepository.findById(savedPayment.uuid).get()
      assertThat(updated.billingAddress).isNotNull()
      assertThat(updated.billingAddress!!.line1).isEqualTo("10 Downing Street")
      assertThat(updated.billingAddress!!.city).isEqualTo("London")
    }

    @Test
    @DisplayName("PAY-039 - received_at can be explicitly set on taken transition")
    fun `should accept explicit received_at on taken transition`() {
      val savedPayment = createPendingPayment()
      val creditId = savedPayment.credit!!.id!!

      webTestClient.patch()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "taken", "received_at": "2024-01-15T10:30:00"}""")
        .exchange()
        .expectStatus().isOk

      val updatedCredit = creditRepository.findById(creditId).get()
      assertThat(updatedCredit.receivedAt).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
    }
  }

  // -------------------------------------------------------------------------
  // PAY-040 to PAY-045: GET /payments/ and GET /payments/{uuid}/
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /payments/ and GET /payments/{uuid}/ (PAY-040 to PAY-045)")
  inner class ListAndRetrievePayment {

    @Test
    @DisplayName("PAY-040 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated GET`() {
      webTestClient.get()
        .uri("/payments/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PAY-040 - GET /payments/ lists only pending payments")
    fun `should list only pending payments`() {
      // Create pending payment
      val credit1 = Credit(amount = 1000L, prisonerNumber = "A1234BC", resolution = CreditResolution.INITIAL)
      credit1.source = CreditSource.ONLINE
      val savedCredit1 = creditRepository.save(credit1)
      val pendingPayment = Payment(amount = 1000L, status = "pending", credit = savedCredit1)
      paymentRepository.save(pendingPayment)

      // Create taken payment
      val credit2 = Credit(amount = 2000L, prisonerNumber = "B5678DE", resolution = CreditResolution.PENDING)
      credit2.source = CreditSource.ONLINE
      val savedCredit2 = creditRepository.save(credit2)
      val takenPayment = Payment(amount = 2000L, status = "taken", credit = savedCredit2)
      paymentRepository.save(takenPayment)

      webTestClient.get()
        .uri("/payments/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results.length()").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("pending")
    }

    @Test
    @DisplayName("PAY-041 - Filter pending payments by modified__lt")
    fun `should filter pending payments by modified_lt`() {
      val credit = Credit(amount = 1000L, prisonerNumber = "A1234BC", resolution = CreditResolution.INITIAL)
      credit.source = CreditSource.ONLINE
      val savedCredit = creditRepository.save(credit)
      val payment = Payment(amount = 1000L, status = "pending", credit = savedCredit)
      paymentRepository.save(payment)

      // Cutoff in the far future should include the payment
      webTestClient.get()
        .uri("/payments/?modified__lt=2099-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results.length()").isEqualTo(1)
    }

    @Test
    @DisplayName("PAY-044 - GET /payments/{uuid}/ retrieves single payment")
    fun `should retrieve single payment by uuid`() {
      val credit = Credit(amount = 1000L, prisonerNumber = "A1234BC", resolution = CreditResolution.INITIAL)
      credit.source = CreditSource.ONLINE
      val savedCredit = creditRepository.save(credit)
      val payment = Payment(amount = 1000L, status = "pending", credit = savedCredit)
      val savedPayment = paymentRepository.save(payment)

      webTestClient.get()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.uuid").isEqualTo(savedPayment.uuid.toString())
        .jsonPath("$.status").isEqualTo("pending")
        .jsonPath("$.amount").isEqualTo(1000)
    }

    @Test
    @DisplayName("PAY-044 - Returns 404 for non-existent payment UUID")
    fun `should return 404 for non-existent payment UUID on GET`() {
      webTestClient.get()
        .uri("/payments/00000000-0000-0000-0000-000000000001/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    @DisplayName("PAY-045 - security_check field is null when no check exists")
    fun `should return null security_check when none exists`() {
      val credit = Credit(amount = 1000L, prisonerNumber = "A1234BC", resolution = CreditResolution.INITIAL)
      credit.source = CreditSource.ONLINE
      val savedCredit = creditRepository.save(credit)
      val payment = Payment(amount = 1000L, status = "pending", credit = savedCredit)
      val savedPayment = paymentRepository.save(payment)

      webTestClient.get()
        .uri("/payments/${savedPayment.uuid}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.security_check").isEmpty
    }
  }

  // -------------------------------------------------------------------------
  // PAY-050 to PAY-053: GET /batches/ (payment amount aggregation)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /payments/batches/ (PAY-050 to PAY-053)")
  inner class ListPaymentBatches {

    @Test
    @DisplayName("PAY-050 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated GET payments batches`() {
      webTestClient.get()
        .uri("/payments/batches/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PAY-050 - Requires ROLE_BANK_ADMIN, returns 403 for missing role")
    fun `should return 403 without ROLE_BANK_ADMIN`() {
      webTestClient.get()
        .uri("/payments/batches/")
        .headers(setAuthorisation(roles = listOf("ROLE_OTHER")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PAY-050 - GET /payments/batches/ lists all payment batches with payment_amount sum")
    fun `should list payment batches with aggregated payment_amount`() {
      // Create a payment batch via reconcile first
      val credit1 = Credit(
        amount = 1000L,
        prisonerNumber = "A1234BC",
        resolution = CreditResolution.PENDING,
      )
      credit1.source = CreditSource.ONLINE
      credit1.receivedAt = LocalDateTime.of(2024, 1, 15, 10, 0, 0)
      val savedCredit1 = creditRepository.save(credit1)

      val payment1 = Payment(amount = 1000L, status = "taken", credit = savedCredit1)
      paymentRepository.save(payment1)

      // Reconcile to create a batch
      webTestClient.post()
        .uri("/payments/batches/reconcile/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"received_at__gte": "2024-01-01T00:00:00", "received_at__lt": "2024-02-01T00:00:00"}""",
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get()
        .uri("/payments/batches/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].payment_amount").isEqualTo(1000)
    }
  }

  // -------------------------------------------------------------------------
  // PAY-060 to PAY-065: POST /payments/batches/reconcile/
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /payments/batches/reconcile/ (PAY-060 to PAY-065)")
  inner class ReconcilePayments {

    @Test
    @DisplayName("PAY-060 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated reconcile`() {
      webTestClient.post()
        .uri("/payments/batches/reconcile/")
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"received_at__gte": "2024-01-01T00:00:00", "received_at__lt": "2024-02-01T00:00:00"}""",
        )
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PAY-060 - Requires ROLE_BANK_ADMIN, returns 403 for missing role")
    fun `should return 403 without ROLE_BANK_ADMIN for reconcile`() {
      webTestClient.post()
        .uri("/payments/batches/reconcile/")
        .headers(setAuthorisation(roles = listOf("ROLE_OTHER")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"received_at__gte": "2024-01-01T00:00:00", "received_at__lt": "2024-02-01T00:00:00"}""",
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PAY-060 - Reconcile creates batch from taken payments in date range and returns 201")
    fun `should create batch from taken payments in date range`() {
      val credit = Credit(
        amount = 1500L,
        prisonerNumber = "A1234BC",
        resolution = CreditResolution.PENDING,
      )
      credit.source = CreditSource.ONLINE
      credit.receivedAt = LocalDateTime.of(2024, 1, 15, 10, 0, 0)
      val savedCredit = creditRepository.save(credit)

      val payment = Payment(amount = 1500L, status = "taken", credit = savedCredit)
      paymentRepository.save(payment)

      webTestClient.post()
        .uri("/payments/batches/reconcile/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"received_at__gte": "2024-01-01T00:00:00", "received_at__lt": "2024-02-01T00:00:00"}""",
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.ref_code").isEqualTo(1)

      val updatedCredit = creditRepository.findById(savedCredit.id!!).get()
      assertThat(updatedCredit.reconciled).isTrue()
    }

    @Test
    @DisplayName("PAY-065 - No batch created when no payments in date range")
    fun `should return 204 when no payments in date range`() {
      webTestClient.post()
        .uri("/payments/batches/reconcile/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"received_at__gte": "2024-01-01T00:00:00", "received_at__lt": "2024-02-01T00:00:00"}""",
        )
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("PAY-063 - ref_code auto-increments from max + 1")
    fun `should auto-increment ref_code from previous max`() {
      // First reconcile
      val credit1 = Credit(amount = 1000L, prisonerNumber = "A1234BC", resolution = CreditResolution.PENDING)
      credit1.source = CreditSource.ONLINE
      credit1.receivedAt = LocalDateTime.of(2024, 1, 15, 10, 0, 0)
      creditRepository.save(credit1)
      paymentRepository.save(Payment(amount = 1000L, status = "taken", credit = credit1))

      webTestClient.post()
        .uri("/payments/batches/reconcile/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"received_at__gte": "2024-01-01T00:00:00", "received_at__lt": "2024-02-01T00:00:00"}""",
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.ref_code").isEqualTo(1)

      // Second reconcile with new credits
      val credit2 = Credit(amount = 2000L, prisonerNumber = "B5678DE", resolution = CreditResolution.PENDING)
      credit2.source = CreditSource.ONLINE
      credit2.receivedAt = LocalDateTime.of(2024, 2, 15, 10, 0, 0)
      creditRepository.save(credit2)
      paymentRepository.save(Payment(amount = 2000L, status = "taken", credit = credit2))

      webTestClient.post()
        .uri("/payments/batches/reconcile/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{"received_at__gte": "2024-02-01T00:00:00", "received_at__lt": "2024-03-01T00:00:00"}""",
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.ref_code").isEqualTo(2)
    }
  }
}

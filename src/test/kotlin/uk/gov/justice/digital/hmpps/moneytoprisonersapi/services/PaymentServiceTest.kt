package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.BillingAddressUpdateDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreatePaymentRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ReconcilePaymentsRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdatePaymentRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.BillingAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BillingAddressRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

  private val paymentRepository: PaymentRepository = mock()
  private val creditRepository: CreditRepository = mock()
  private val billingAddressRepository: BillingAddressRepository = mock()
  private val paymentBatchRepository: PaymentBatchRepository = mock()
  private lateinit var paymentService: PaymentService

  @BeforeEach
  fun setUp() {
    paymentService = PaymentService(
      paymentRepository = paymentRepository,
      creditRepository = creditRepository,
      billingAddressRepository = billingAddressRepository,
      paymentBatchRepository = paymentBatchRepository,
    )
  }

  private fun makeCredit(resolution: CreditResolution = CreditResolution.INITIAL): Credit {
    val credit = Credit(amount = 1000L, prisonerNumber = "A1234BC")
    credit.source = CreditSource.ONLINE
    credit.resolution = resolution
    return credit
  }

  private fun makePayment(status: String = "pending", credit: Credit? = null): Payment {
    val c = credit ?: makeCredit()
    return Payment(
      amount = 1000L,
      status = status,
      credit = c,
    )
  }

  // -------------------------------------------------------------------------
  // PAY-020 to PAY-027: Payment Creation
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("PAY-020 to PAY-027: Payment Creation")
  inner class CreatePayment {

    @Test
    @DisplayName("PAY-020 - Creates payment with status=pending and linked credit with source=ONLINE resolution=INITIAL")
    fun `should create payment with pending status and INITIAL credit`() {
      val request = CreatePaymentRequest(
        prisonerNumber = "A1234BC",
        prisonerDob = LocalDate.of(1990, 1, 1),
        amount = 1000L,
      )

      val savedCredit = makeCredit(CreditResolution.INITIAL)
      val savedPayment = Payment(amount = 1000L, status = "pending", credit = savedCredit)

      `when`(creditRepository.save(any())).thenReturn(savedCredit)
      `when`(paymentRepository.save(any())).thenReturn(savedPayment)

      val result = paymentService.createPayment(request)

      assertThat(result.status).isEqualTo("pending")
      assertThat(result.amount).isEqualTo(1000L)

      val creditCaptor = argumentCaptor<Credit>()
      verify(creditRepository).save(creditCaptor.capture())
      val capturedCredit = creditCaptor.firstValue
      assertThat(capturedCredit.source).isEqualTo(CreditSource.ONLINE)
      assertThat(capturedCredit.resolution).isEqualTo(CreditResolution.INITIAL)
      assertThat(capturedCredit.prisonerNumber).isEqualTo("A1234BC")
      assertThat(capturedCredit.prisonerDob).isEqualTo(LocalDate.of(1990, 1, 1))
      assertThat(capturedCredit.amount).isEqualTo(1000L)
    }

    @Test
    @DisplayName("PAY-022 - Missing prisoner_number raises validation error")
    fun `should fail when prisoner_number is missing`() {
      val request = CreatePaymentRequest(
        prisonerNumber = "",
        prisonerDob = LocalDate.of(1990, 1, 1),
        amount = 1000L,
      )

      assertThatThrownBy { paymentService.createPayment(request) }
        .isInstanceOf(PaymentValidationException::class.java)
    }

    @Test
    @DisplayName("PAY-023 - Missing prisoner_dob raises validation error")
    fun `should fail when prisoner_dob is missing`() {
      val request = CreatePaymentRequest(
        prisonerNumber = "A1234BC",
        prisonerDob = null,
        amount = 1000L,
      )

      assertThatThrownBy { paymentService.createPayment(request) }
        .isInstanceOf(PaymentValidationException::class.java)
    }

    @Test
    @DisplayName("PAY-024 - Amount must be positive")
    fun `should fail when amount is zero or negative`() {
      val request = CreatePaymentRequest(
        prisonerNumber = "A1234BC",
        prisonerDob = LocalDate.of(1990, 1, 1),
        amount = 0L,
      )

      assertThatThrownBy { paymentService.createPayment(request) }
        .isInstanceOf(PaymentValidationException::class.java)
    }
  }

  // -------------------------------------------------------------------------
  // PAY-030 to PAY-038: Payment Update (PATCH)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("PAY-030 to PAY-038: Payment Update")
  inner class UpdatePayment {

    @Test
    @DisplayName("PAY-030 - Returns 404 for non-existent payment UUID")
    fun `should throw not found for missing payment`() {
      val uuid = UUID.randomUUID()
      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.empty())

      assertThatThrownBy { paymentService.updatePayment(uuid, UpdatePaymentRequest()) }
        .isInstanceOf(PaymentNotFoundException::class.java)
    }

    @Test
    @DisplayName("PAY-031 - Non-pending payment update throws PaymentNotPendingException")
    fun `should throw conflict when payment status is not pending`() {
      val uuid = UUID.randomUUID()
      val credit = makeCredit(CreditResolution.PENDING)
      val payment = makePayment(status = "taken", credit = credit)

      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.of(payment))

      assertThatThrownBy { paymentService.updatePayment(uuid, UpdatePaymentRequest(status = "failed")) }
        .isInstanceOf(PaymentNotPendingException::class.java)
        .hasMessageContaining("taken")
    }

    @Test
    @DisplayName("PAY-032 - Transition to taken sets credit resolution to PENDING and received_at")
    fun `should transition credit to PENDING when payment status becomes taken`() {
      val uuid = UUID.randomUUID()
      val credit = makeCredit(CreditResolution.INITIAL)
      val payment = makePayment(status = "pending", credit = credit)

      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.of(payment))
      `when`(creditRepository.save(any())).thenReturn(credit)
      `when`(paymentRepository.save(any())).thenReturn(payment)

      paymentService.updatePayment(uuid, UpdatePaymentRequest(status = "taken"))

      val creditCaptor = argumentCaptor<Credit>()
      verify(creditRepository).save(creditCaptor.capture())
      val savedCredit = creditCaptor.firstValue
      assertThat(savedCredit.resolution).isEqualTo(CreditResolution.PENDING)
      assertThat(savedCredit.receivedAt).isNotNull()
    }

    @Test
    @DisplayName("PAY-033 - Transition to failed does not change credit resolution")
    fun `should leave credit as INITIAL when payment status becomes failed`() {
      val uuid = UUID.randomUUID()
      val credit = makeCredit(CreditResolution.INITIAL)
      val payment = makePayment(status = "pending", credit = credit)

      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.of(payment))
      `when`(paymentRepository.save(any())).thenReturn(payment)

      paymentService.updatePayment(uuid, UpdatePaymentRequest(status = "failed"))

      verify(creditRepository, never()).save(any())
      assertThat(credit.resolution).isEqualTo(CreditResolution.INITIAL)
    }

    @Test
    @DisplayName("PAY-034 - Transition to rejected sets credit resolution to FAILED")
    fun `should set credit resolution to FAILED when payment status becomes rejected`() {
      val uuid = UUID.randomUUID()
      val credit = makeCredit(CreditResolution.INITIAL)
      val payment = makePayment(status = "pending", credit = credit)

      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.of(payment))
      `when`(creditRepository.save(any())).thenReturn(credit)
      `when`(paymentRepository.save(any())).thenReturn(payment)

      paymentService.updatePayment(uuid, UpdatePaymentRequest(status = "rejected"))

      val creditCaptor = argumentCaptor<Credit>()
      verify(creditRepository).save(creditCaptor.capture())
      assertThat(creditCaptor.firstValue.resolution).isEqualTo(CreditResolution.FAILED)
    }

    @Test
    @DisplayName("PAY-035 - Transition to expired sets credit resolution to FAILED")
    fun `should set credit resolution to FAILED when payment status becomes expired`() {
      val uuid = UUID.randomUUID()
      val credit = makeCredit(CreditResolution.INITIAL)
      val payment = makePayment(status = "pending", credit = credit)

      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.of(payment))
      `when`(creditRepository.save(any())).thenReturn(credit)
      `when`(paymentRepository.save(any())).thenReturn(payment)

      paymentService.updatePayment(uuid, UpdatePaymentRequest(status = "expired"))

      val creditCaptor = argumentCaptor<Credit>()
      verify(creditRepository).save(creditCaptor.capture())
      assertThat(creditCaptor.firstValue.resolution).isEqualTo(CreditResolution.FAILED)
    }

    @Test
    @DisplayName("PAY-036 - Card details can be added on PATCH")
    fun `should update card details on PATCH`() {
      val uuid = UUID.randomUUID()
      val credit = makeCredit(CreditResolution.INITIAL)
      val payment = makePayment(status = "pending", credit = credit)

      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.of(payment))
      `when`(paymentRepository.save(any())).thenReturn(payment)

      val request = UpdatePaymentRequest(
        cardholderName = "John Doe",
        cardNumberFirstDigits = "411111",
        cardNumberLastDigits = "1234",
        cardExpiryDate = "12/25",
        cardBrand = "Visa",
      )

      paymentService.updatePayment(uuid, request)

      val paymentCaptor = argumentCaptor<Payment>()
      verify(paymentRepository).save(paymentCaptor.capture())
      val saved = paymentCaptor.firstValue
      assertThat(saved.cardholderName).isEqualTo("John Doe")
      assertThat(saved.cardNumberFirstDigits).isEqualTo("411111")
      assertThat(saved.cardNumberLastDigits).isEqualTo("1234")
      assertThat(saved.cardExpiryDate).isEqualTo("12/25")
      assertThat(saved.cardBrand).isEqualTo("Visa")
    }

    @Test
    @DisplayName("PAY-037 - Billing address is created on first PATCH and linked to payment")
    fun `should create billing address on first PATCH`() {
      val uuid = UUID.randomUUID()
      val credit = makeCredit(CreditResolution.INITIAL)
      val payment = makePayment(status = "pending", credit = credit)

      val savedAddress = BillingAddress(
        id = 1L,
        line1 = "10 Downing Street",
        city = "London",
        postcode = "SW1A 2AA",
        country = "GB",
      )

      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.of(payment))
      `when`(billingAddressRepository.save(any())).thenReturn(savedAddress)
      `when`(paymentRepository.save(any())).thenReturn(payment)

      val request = UpdatePaymentRequest(
        billingAddress = BillingAddressUpdateDto(
          line1 = "10 Downing Street",
          city = "London",
          postcode = "SW1A 2AA",
          country = "GB",
        ),
      )

      paymentService.updatePayment(uuid, request)

      val addressCaptor = argumentCaptor<BillingAddress>()
      verify(billingAddressRepository).save(addressCaptor.capture())
      val savedAddr = addressCaptor.firstValue
      assertThat(savedAddr.line1).isEqualTo("10 Downing Street")
      assertThat(savedAddr.city).isEqualTo("London")
    }

    @Test
    @DisplayName("PAY-038 - Existing billing address is updated in-place on subsequent PATCH")
    fun `should update existing billing address in-place`() {
      val uuid = UUID.randomUUID()
      val credit = makeCredit(CreditResolution.INITIAL)
      val existingAddress = BillingAddress(id = 5L, line1 = "Old Street", city = "Old City")
      val payment = makePayment(status = "pending", credit = credit)
      payment.billingAddress = existingAddress

      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.of(payment))
      `when`(billingAddressRepository.save(any())).thenReturn(existingAddress)
      `when`(paymentRepository.save(any())).thenReturn(payment)

      val request = UpdatePaymentRequest(
        billingAddress = BillingAddressUpdateDto(
          line1 = "New Street",
          city = "New City",
        ),
      )

      paymentService.updatePayment(uuid, request)

      val addressCaptor = argumentCaptor<BillingAddress>()
      verify(billingAddressRepository).save(addressCaptor.capture())
      val saved = addressCaptor.firstValue
      assertThat(saved.id).isEqualTo(5L)
      assertThat(saved.line1).isEqualTo("New Street")
      assertThat(saved.city).isEqualTo("New City")
    }

    @Test
    @DisplayName("PAY-039 - received_at can be set explicitly when transitioning to taken")
    fun `should set explicit received_at when transitioning to taken`() {
      val uuid = UUID.randomUUID()
      val credit = makeCredit(CreditResolution.INITIAL)
      val payment = makePayment(status = "pending", credit = credit)
      val explicitReceivedAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0)

      `when`(paymentRepository.findById(uuid)).thenReturn(Optional.of(payment))
      `when`(creditRepository.save(any())).thenReturn(credit)
      `when`(paymentRepository.save(any())).thenReturn(payment)

      paymentService.updatePayment(uuid, UpdatePaymentRequest(status = "taken", receivedAt = explicitReceivedAt))

      val creditCaptor = argumentCaptor<Credit>()
      verify(creditRepository).save(creditCaptor.capture())
      assertThat(creditCaptor.firstValue.receivedAt).isEqualTo(explicitReceivedAt)
    }
  }

  // -------------------------------------------------------------------------
  // PAY-040 to PAY-045: Payment List
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("PAY-040 to PAY-045: Payment List")
  inner class ListPayments {

    @Test
    @DisplayName("PAY-040 - Lists only pending payments")
    fun `should list only pending payments`() {
      val credit1 = makeCredit(CreditResolution.INITIAL)
      val credit2 = makeCredit(CreditResolution.PENDING)
      val pendingPayment = makePayment(status = "pending", credit = credit1)
      val takenPayment = makePayment(status = "taken", credit = credit2)

      `when`(paymentRepository.findByStatus("pending")).thenReturn(listOf(pendingPayment))

      val result = paymentService.listPendingPayments(modifiedLt = null)

      assertThat(result).hasSize(1)
      assertThat(result[0].status).isEqualTo("pending")
    }

    @Test
    @DisplayName("PAY-041 - Can filter by modified__lt")
    fun `should filter pending payments by modified_lt`() {
      val cutoff = LocalDateTime.of(2024, 1, 10, 0, 0, 0)
      val credit = makeCredit(CreditResolution.INITIAL)
      val payment = makePayment(status = "pending", credit = credit)

      `when`(paymentRepository.findByStatusAndModifiedBefore("pending", cutoff)).thenReturn(listOf(payment))

      val result = paymentService.listPendingPayments(modifiedLt = cutoff)

      assertThat(result).hasSize(1)
    }
  }

  // -------------------------------------------------------------------------
  // PAY-060 to PAY-065: Payment Reconciliation
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("PAY-060 to PAY-065: Payment Reconciliation")
  inner class ReconcilePayments {

    @Test
    @DisplayName("PAY-060 - Creates batch with taken payments in the date range")
    fun `should create batch with taken payments in date range`() {
      val from = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
      val to = LocalDateTime.of(2024, 1, 31, 23, 59, 59)
      val request = ReconcilePaymentsRequest(receivedAtGte = from, receivedAtLt = to)

      val credit = makeCredit(CreditResolution.PENDING)
      credit.receivedAt = LocalDateTime.of(2024, 1, 15, 10, 0, 0)
      val payment = makePayment(status = "taken", credit = credit)

      `when`(
        creditRepository.findByResolutionAndReconciledFalseAndReceivedAtGreaterThanEqualAndReceivedAtBefore(
          CreditResolution.PENDING,
          from,
          to,
        ),
      ).thenReturn(listOf(credit))
      `when`(paymentBatchRepository.findMaxRefCode()).thenReturn(null)
      `when`(paymentBatchRepository.save(any())).thenAnswer { invocation -> invocation.arguments[0] }

      val result = paymentService.reconcilePayments(request)

      assertThat(result).isNotNull()
    }

    @Test
    @DisplayName("PAY-065 - Returns null when no matching payments")
    fun `should return null when no payments in date range`() {
      val from = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
      val to = LocalDateTime.of(2024, 1, 31, 23, 59, 59)
      val request = ReconcilePaymentsRequest(receivedAtGte = from, receivedAtLt = to)

      `when`(
        creditRepository.findByResolutionAndReconciledFalseAndReceivedAtGreaterThanEqualAndReceivedAtBefore(
          CreditResolution.PENDING,
          from,
          to,
        ),
      ).thenReturn(emptyList())

      val result = paymentService.reconcilePayments(request)

      assertThat(result).isNull()
      verify(paymentBatchRepository, never()).save(any())
    }

    @Test
    @DisplayName("PAY-063 - ref_code auto-increments from max + 1")
    fun `should auto-increment ref_code from max plus 1`() {
      val from = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
      val to = LocalDateTime.of(2024, 1, 31, 23, 59, 59)
      val request = ReconcilePaymentsRequest(receivedAtGte = from, receivedAtLt = to)

      val credit = makeCredit(CreditResolution.PENDING)
      credit.receivedAt = LocalDateTime.of(2024, 1, 15, 10, 0, 0)

      `when`(
        creditRepository.findByResolutionAndReconciledFalseAndReceivedAtGreaterThanEqualAndReceivedAtBefore(
          CreditResolution.PENDING,
          from,
          to,
        ),
      ).thenReturn(listOf(credit))
      `when`(paymentBatchRepository.findMaxRefCode()).thenReturn(5)
      `when`(paymentBatchRepository.save(any())).thenAnswer { invocation -> invocation.arguments[0] }

      paymentService.reconcilePayments(request)

      val batchCaptor = argumentCaptor<uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PaymentBatch>()
      verify(paymentBatchRepository).save(batchCaptor.capture())
      assertThat(batchCaptor.firstValue.refCode).isEqualTo(6)
    }

    @Test
    @DisplayName("PAY-064 - Credits are marked as reconciled")
    fun `should mark credits as reconciled`() {
      val from = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
      val to = LocalDateTime.of(2024, 1, 31, 23, 59, 59)
      val request = ReconcilePaymentsRequest(receivedAtGte = from, receivedAtLt = to)

      val credit = makeCredit(CreditResolution.PENDING)
      credit.receivedAt = LocalDateTime.of(2024, 1, 15, 10, 0, 0)

      `when`(
        creditRepository.findByResolutionAndReconciledFalseAndReceivedAtGreaterThanEqualAndReceivedAtBefore(
          CreditResolution.PENDING,
          from,
          to,
        ),
      ).thenReturn(listOf(credit))
      `when`(paymentBatchRepository.findMaxRefCode()).thenReturn(null)
      `when`(paymentBatchRepository.save(any())).thenAnswer { invocation -> invocation.arguments[0] }

      paymentService.reconcilePayments(request)

      val creditCaptor = argumentCaptor<Credit>()
      verify(creditRepository).save(creditCaptor.capture())
      assertThat(creditCaptor.firstValue.reconciled).isTrue()
    }
  }
}

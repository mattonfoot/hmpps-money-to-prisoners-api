package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreatePaymentRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ReconcilePaymentsRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdatePaymentRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.BillingAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PaymentBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BillingAddressRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentRepository
import java.time.LocalDateTime
import java.util.UUID

class PaymentNotFoundException(uuid: UUID) : RuntimeException("Payment not found with uuid: $uuid")

class PaymentNotPendingException(currentStatus: String) : RuntimeException("Payment cannot be updated in status \"$currentStatus\"")

class PaymentValidationException(message: String) : RuntimeException(message)

private val VALID_PAYMENT_STATUSES = setOf("pending", "taken", "failed", "rejected", "expired")

@Service
class PaymentService(
  private val paymentRepository: PaymentRepository,
  private val creditRepository: CreditRepository,
  private val billingAddressRepository: BillingAddressRepository,
  private val paymentBatchRepository: PaymentBatchRepository,
) {

  @Transactional
  fun createPayment(request: CreatePaymentRequest): Payment {
    if (request.prisonerNumber.isNullOrBlank()) {
      throw PaymentValidationException("prisoner_number is required")
    }
    if (request.prisonerDob == null) {
      throw PaymentValidationException("prisoner_dob is required")
    }
    if (request.amount == null || request.amount <= 0) {
      throw PaymentValidationException("amount must be a positive integer")
    }

    val credit = Credit(
      amount = request.amount,
      prisonerNumber = request.prisonerNumber,
      prisonerDob = request.prisonerDob,
      resolution = CreditResolution.INITIAL,
    )
    credit.source = CreditSource.ONLINE

    val savedCredit = creditRepository.save(credit)

    val payment = Payment(
      amount = request.amount,
      status = "pending",
      credit = savedCredit,
      ipAddress = request.ipAddress,
    )

    return paymentRepository.save(payment)
  }

  @Transactional
  fun updatePayment(uuid: UUID, request: UpdatePaymentRequest): Payment {
    val payment = paymentRepository.findById(uuid)
      .orElseThrow { PaymentNotFoundException(uuid) }

    if (payment.status != "pending") {
      throw PaymentNotPendingException(payment.status ?: "unknown")
    }

    // Handle status transition
    val newStatus = request.status
    if (newStatus != null) {
      when (newStatus) {
        "taken" -> {
          val credit = payment.credit!!
          credit.resolution = CreditResolution.PENDING
          credit.receivedAt = request.receivedAt ?: LocalDateTime.now()
          creditRepository.save(credit)
        }
        "rejected", "expired" -> {
          val credit = payment.credit!!
          credit.resolution = CreditResolution.FAILED
          creditRepository.save(credit)
        }
        "failed" -> {
          // Credit stays as INITIAL (no change)
        }
      }
      payment.status = newStatus
    }

    // Update card details
    request.cardholderName?.let { payment.cardholderName = it }
    request.cardNumberFirstDigits?.let { payment.cardNumberFirstDigits = it }
    request.cardNumberLastDigits?.let { payment.cardNumberLastDigits = it }
    request.cardExpiryDate?.let { payment.cardExpiryDate = it }
    request.cardBrand?.let { payment.cardBrand = it }
    request.ipAddress?.let { payment.ipAddress = it }

    // Handle billing address
    val billingAddressUpdate = request.billingAddress
    if (billingAddressUpdate != null) {
      val existingAddress = payment.billingAddress
      val addressToSave = if (existingAddress != null) {
        // Update existing address in-place
        existingAddress.line1 = billingAddressUpdate.line1 ?: existingAddress.line1
        existingAddress.line2 = billingAddressUpdate.line2 ?: existingAddress.line2
        existingAddress.city = billingAddressUpdate.city ?: existingAddress.city
        existingAddress.country = billingAddressUpdate.country ?: existingAddress.country
        existingAddress.postcode = billingAddressUpdate.postcode ?: existingAddress.postcode
        existingAddress
      } else {
        // Create new billing address
        BillingAddress(
          line1 = billingAddressUpdate.line1,
          line2 = billingAddressUpdate.line2,
          city = billingAddressUpdate.city,
          country = billingAddressUpdate.country,
          postcode = billingAddressUpdate.postcode,
        )
      }
      val savedAddress = billingAddressRepository.save(addressToSave)
      payment.billingAddress = savedAddress
    }

    return paymentRepository.save(payment)
  }

  fun listPendingPayments(modifiedLt: LocalDateTime?): List<Payment> = if (modifiedLt != null) {
    paymentRepository.findByStatusAndModifiedBefore("pending", modifiedLt)
  } else {
    paymentRepository.findByStatus("pending")
  }

  fun getPayment(uuid: UUID): Payment = paymentRepository.findById(uuid)
    .orElseThrow { PaymentNotFoundException(uuid) }

  fun listPaymentBatches(settlementDate: java.time.LocalDate?): List<PaymentBatch> = if (settlementDate != null) {
    paymentBatchRepository.findBySettlementDate(settlementDate)
  } else {
    paymentBatchRepository.findAll()
  }

  @Transactional
  fun reconcilePayments(request: ReconcilePaymentsRequest): PaymentBatch? {
    val credits = creditRepository.findByResolutionAndReconciledFalseAndReceivedAtGreaterThanEqualAndReceivedAtBefore(
      CreditResolution.PENDING,
      request.receivedAtGte,
      request.receivedAtLt,
    )

    if (credits.isEmpty()) {
      return null
    }

    val maxRefCode = paymentBatchRepository.findMaxRefCode() ?: 0
    val newRefCode = maxRefCode + 1

    // Mark all credits as reconciled
    for (credit in credits) {
      credit.reconciled = true
      creditRepository.save(credit)
    }

    val batch = PaymentBatch(
      refCode = newRefCode,
      credits = credits.toMutableList(),
    )

    return paymentBatchRepository.save(batch)
  }
}

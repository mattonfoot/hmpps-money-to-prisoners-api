package uk.gov.justice.digital.hmpps.moneytoprisonersapi

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import java.time.LocalDate
import java.util.UUID

open class CustomException constructor(message: String, val status: HttpStatus) : RuntimeException(message)

// ── account ─────────────────────────────────────────────────────────────────

class DuplicateBalanceDateException(date: LocalDate) :
  CustomException("Balance exists for date $date", HttpStatus.BAD_REQUEST)

// ── credit ──────────────────────────────────────────────────────────────────

class CreditNotFoundException(id: Long) :
  CustomException("Credit not found with id: $id", HttpStatus.NOT_FOUND)

class InvalidCreditStateException(from: CreditResolution, to: CreditResolution) :
  CustomException("Cannot transition credit from $from to $to", HttpStatus.BAD_REQUEST)

// ── payment ─────────────────────────────────────────────────────────────────

class PaymentNotFoundException(uuid: UUID) :
  CustomException("Payment not found with uuid: $uuid", HttpStatus.NOT_FOUND)

class PaymentNotPendingException(currentStatus: String) :
  CustomException("Payment cannot be updated in status \"$currentStatus\"", HttpStatus.BAD_REQUEST)

class PaymentValidationException(message: String) :
  CustomException(message, HttpStatus.BAD_REQUEST)

// ── disbursement ────────────────────────────────────────────────────────────

class DisbursementNotFoundException(id: Long) :
  CustomException("Disbursement not found with id: $id", HttpStatus.NOT_FOUND)

class DisbursementNotPendingException(id: Long, resolution: DisbursementResolution) :
  CustomException("Disbursement $id is not PENDING (current resolution: $resolution)", HttpStatus.BAD_REQUEST)

class InvalidDisbursementStateException(from: DisbursementResolution, to: DisbursementResolution) :
  CustomException("Cannot transition disbursement from $from to $to", HttpStatus.BAD_REQUEST)

// ── security ────────────────────────────────────────────────────────────────

class SecurityCheckConflictException(message: String) :
  CustomException(message, HttpStatus.BAD_REQUEST)

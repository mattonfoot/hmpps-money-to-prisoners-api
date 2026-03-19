package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionSource

enum class TransactionStatus(@JsonValue val value: String) {
  CREDITABLE("creditable"),
  REFUNDABLE("refundable"),
  UNIDENTIFIED("unidentified"),
  ANONYMOUS("anonymous"),
  ANOMALOUS("anomalous"),
  UNKNOWN("unknown"),
  ;

  companion object {
    fun computeFrom(transaction: Transaction): TransactionStatus {
      val credit = transaction.credit

      // TXN-014: Anomalous — credit category + administrative source
      if (transaction.category == TransactionCategory.CREDIT && transaction.source == TransactionSource.ADMINISTRATIVE) {
        return ANOMALOUS
      }

      // TXN-013: Anonymous — incomplete sender, bank_transfer, no credit
      if (transaction.incompleteSenderInfo && transaction.source == TransactionSource.BANK_TRANSFER && credit == null) {
        return ANONYMOUS
      }

      // TXN-012: Unidentified — incomplete sender, no prison, bank_transfer credit
      if (transaction.incompleteSenderInfo && credit != null && credit.prison == null && transaction.source == TransactionSource.BANK_TRANSFER) {
        return UNIDENTIFIED
      }

      // TXN-010: Creditable — credit exists, prison assigned, not blocked
      if (credit != null && credit.prison != null && !credit.blocked) {
        return CREDITABLE
      }

      // TXN-011: Refundable — credit exists, sender info complete, no prison or blocked
      if (credit != null && !transaction.incompleteSenderInfo && (credit.prison == null || credit.blocked)) {
        return REFUNDABLE
      }

      return UNKNOWN
    }
  }
}

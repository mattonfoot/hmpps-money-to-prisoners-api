package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution

enum class CreditStatus(@JsonValue val value: String) {
  INITIAL("initial"),
  CREDIT_PENDING("credit_pending"),
  CREDITED("credited"),
  REFUND_PENDING("refund_pending"),
  REFUNDED("refunded"),
  FAILED("failed"),
  ;

  companion object {
    fun computeFrom(credit: Credit): CreditStatus {
      val resolution = credit.resolution
      val pendingValue = CreditResolution.PENDING.value
      val manualValue = CreditResolution.MANUAL.value
      // Django credit_credit has no `incompleteSenderInfo` column — that flag
      // lives on transaction_transaction. Walk through the linked transaction.
      val incompleteSenderInfo = credit.transaction?.incompleteSenderInfo == true
      return when {
        resolution == CreditResolution.CREDITED.value -> CREDITED
        resolution == CreditResolution.REFUNDED.value -> REFUNDED
        resolution == CreditResolution.FAILED.value -> FAILED
        resolution == CreditResolution.INITIAL.value -> INITIAL
        credit.prison != null &&
          !credit.blocked &&
          (resolution == pendingValue || resolution == manualValue) ->
          CREDIT_PENDING
        (credit.prison == null || credit.blocked) &&
          resolution == pendingValue &&
          !incompleteSenderInfo ->
          REFUND_PENDING
        else -> INITIAL
      }
    }
  }
}

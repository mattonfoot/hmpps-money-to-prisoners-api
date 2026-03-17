package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution

enum class CreditStatus(val value: String) {
  INITIAL("initial"),
  CREDIT_PENDING("credit_pending"),
  CREDITED("credited"),
  REFUND_PENDING("refund_pending"),
  REFUNDED("refunded"),
  FAILED("failed"),
  ;

  companion object {
    fun computeFrom(credit: Credit): CreditStatus = when {
      credit.resolution == CreditResolution.CREDITED -> CREDITED
      credit.resolution == CreditResolution.REFUNDED -> REFUNDED
      credit.resolution == CreditResolution.FAILED -> FAILED
      credit.resolution == CreditResolution.INITIAL -> INITIAL
      credit.prison != null &&
        !credit.blocked &&
        credit.resolution in listOf(CreditResolution.PENDING, CreditResolution.MANUAL) ->
        CREDIT_PENDING
      (credit.prison == null || credit.blocked) &&
        credit.resolution == CreditResolution.PENDING &&
        !credit.incompleteSenderInfo ->
        REFUND_PENDING
      else -> INITIAL
    }
  }
}

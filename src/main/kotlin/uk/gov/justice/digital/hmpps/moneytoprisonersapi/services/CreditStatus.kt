package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

enum class CreditStatus(val value: String) {
  INITIAL("initial"),
  CREDIT_PENDING("credit_pending"),
  CREDITED("credited"),
  REFUND_PENDING("refund_pending"),
  REFUNDED("refunded"),
  FAILED("failed"),
}

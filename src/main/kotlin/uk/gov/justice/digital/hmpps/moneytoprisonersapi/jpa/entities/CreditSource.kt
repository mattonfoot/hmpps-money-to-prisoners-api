package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

enum class CreditSource(val value: String) {
  BANK_TRANSFER("bank_transfer"),
  ONLINE("online"),
  UNKNOWN("unknown"),
}

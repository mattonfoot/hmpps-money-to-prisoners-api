package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import com.fasterxml.jackson.annotation.JsonValue

enum class TransactionSource(@JsonValue val value: String) {
  BANK_TRANSFER("bank_transfer"),
  ADMINISTRATIVE("administrative"),
}

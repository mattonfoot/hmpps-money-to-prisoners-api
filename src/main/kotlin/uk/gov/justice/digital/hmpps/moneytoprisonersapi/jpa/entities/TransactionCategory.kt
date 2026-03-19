package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import com.fasterxml.jackson.annotation.JsonValue

enum class TransactionCategory(@JsonValue val value: String) {
  CREDIT("credit"),
  DEBIT("debit"),
}

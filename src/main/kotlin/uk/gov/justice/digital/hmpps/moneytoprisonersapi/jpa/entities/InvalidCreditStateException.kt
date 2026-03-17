package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

class InvalidCreditStateException(
  from: CreditResolution,
  to: CreditResolution,
) : RuntimeException("Cannot transition credit from $from to $to")

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

class InvalidDisbursementStateException(
  from: DisbursementResolution,
  to: DisbursementResolution,
) : RuntimeException("Cannot transition disbursement from $from to $to")

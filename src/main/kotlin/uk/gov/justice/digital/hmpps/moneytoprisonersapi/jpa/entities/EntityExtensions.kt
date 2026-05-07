package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import uk.gov.justice.digital.hmpps.moneytoprisonersapi.CustomException

// Extension functions that bring back the rich domain methods that lived on
// the pre-regen entity classes. They sit outside the entity files so future
// IntelliJ regens don't clobber them.

class InvalidDisbursementStateException(
  current: DisbursementResolution,
  attempted: DisbursementResolution,
) : CustomException("Cannot transition disbursement from $current to $attempted")

class InvalidCreditResolutionException(
  current: CreditResolution,
  attempted: CreditResolution,
) : CustomException("Cannot transition credit from $current to $attempted")

private fun parseDisbursementResolution(value: String): DisbursementResolution =
  DisbursementResolution.fromValue(value)

private fun parseCreditResolution(value: String): CreditResolution =
  CreditResolution.fromValue(value)

fun DisbursementDisbursement.transitionResolution(newResolution: DisbursementResolution) {
  val current = parseDisbursementResolution(resolution)
  if (current == newResolution) return
  if (!DisbursementResolution.isValidTransition(current, newResolution)) {
    throw InvalidDisbursementStateException(current, newResolution)
  }
  resolution = newResolution.value
}

fun CreditCredit.transitionResolution(newResolution: CreditResolution) {
  val current = parseCreditResolution(resolution)
  if (current == newResolution) return
  if (!CreditResolution.isValidTransition(current, newResolution)) {
    throw InvalidCreditResolutionException(current, newResolution)
  }
  resolution = newResolution.value
}

fun TransactionTransaction.transitionResolution(newResolution: CreditResolution) {
  // Mirror old behaviour: transaction's "resolution" was driven through its credit.
  credit?.transitionResolution(newResolution)
}

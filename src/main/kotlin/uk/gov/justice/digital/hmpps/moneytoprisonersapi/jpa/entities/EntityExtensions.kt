package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.springframework.http.HttpStatus
import org.springframework.scheduling.support.CronExpression
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.CustomException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

// Extension functions that bring back the rich domain methods that lived on
// the pre-regen entity classes. They sit outside the entity files so future
// IntelliJ regens don't clobber them.

class InvalidDisbursementStateException(
  current: DisbursementResolution,
  attempted: DisbursementResolution,
) : CustomException("Cannot transition disbursement from $current to $attempted", HttpStatus.CONFLICT)

class InvalidCreditResolutionException(
  current: CreditResolution,
  attempted: CreditResolution,
) : CustomException("Cannot transition credit from $current to $attempted", HttpStatus.CONFLICT)

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

// Django stores cron in `cron_entry` as a five-field crontab. Spring's
// CronExpression accepts six fields (with seconds), so prefix "0 ".
fun CoreScheduledcommand.updateNextExecution() {
  val expr = CronExpression.parse("0 $cronEntry")
  val next = expr.next(LocalDateTime.now()) ?: return
  nextExecution = next.atOffset(ZoneOffset.UTC)
}


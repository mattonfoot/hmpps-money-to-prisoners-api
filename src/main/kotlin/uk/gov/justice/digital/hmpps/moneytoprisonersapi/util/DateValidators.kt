package uk.gov.justice.digital.hmpps.moneytoprisonersapi.util

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Mirrors Django's `validate_monday` validator (`core.models.validate_monday`).
 *
 * Performance data is keyed by the Monday that starts each week — calls in
 * the API endpoints validate that the supplied date is a Monday before
 * persisting. Throws [IllegalArgumentException] (caught and surfaced as a
 * 400 by the global exception handler) when [date] is any other day.
 */
fun validateMonday(date: LocalDate) {
  if (date.dayOfWeek != DayOfWeek.MONDAY) {
    throw IllegalArgumentException("date must be a Monday, got ${date.dayOfWeek}")
  }
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

enum class DisbursementResolution {
  PENDING,
  PRECONFIRMED,
  CONFIRMED,
  SENT,
  REJECTED,
  ;

  companion object {
    private val VALID_TRANSITIONS: Map<DisbursementResolution, Set<DisbursementResolution>> = mapOf(
      PENDING to setOf(PRECONFIRMED, REJECTED),
      PRECONFIRMED to setOf(CONFIRMED, PENDING, REJECTED),
      CONFIRMED to setOf(SENT),
      SENT to emptySet(),
      REJECTED to setOf(PENDING),
    )

    fun isValidTransition(from: DisbursementResolution, to: DisbursementResolution): Boolean {
      if (from == to) return true // idempotent
      return VALID_TRANSITIONS[from]?.contains(to) ?: false
    }

    val TERMINAL_STATES: Set<DisbursementResolution> = setOf(SENT)
  }
}

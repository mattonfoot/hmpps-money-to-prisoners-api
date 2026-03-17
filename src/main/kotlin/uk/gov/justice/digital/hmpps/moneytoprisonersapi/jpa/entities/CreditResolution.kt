package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

enum class CreditResolution(val value: String) {
  INITIAL("initial"),
  PENDING("pending"),
  MANUAL("manual"),
  CREDITED("credited"),
  REFUNDED("refunded"),
  FAILED("failed"),
  ;

  companion object {
    private val VALID_TRANSITIONS: Map<CreditResolution, Set<CreditResolution>> = mapOf(
      INITIAL to setOf(PENDING, FAILED),
      PENDING to setOf(MANUAL, CREDITED, REFUNDED, FAILED),
      MANUAL to setOf(CREDITED, REFUNDED, FAILED),
      CREDITED to emptySet(),
      REFUNDED to emptySet(),
      FAILED to emptySet(),
    )

    fun isValidTransition(from: CreditResolution, to: CreditResolution): Boolean = VALID_TRANSITIONS[from]?.contains(to) ?: false

    val TERMINAL_STATES: Set<CreditResolution> = setOf(CREDITED, REFUNDED, FAILED)
  }
}

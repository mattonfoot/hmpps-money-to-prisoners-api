package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

enum class EmailFrequency(val value: String) {
  DAILY("daily"),
  NEVER("never"),
  ;

  companion object {
    fun fromValue(value: String): EmailFrequency? = entries.firstOrNull { it.value == value }
  }
}

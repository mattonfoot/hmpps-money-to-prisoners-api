package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class DisbursementResolution(@JsonValue val value: String) {
  PENDING("pending"),
  PRECONFIRMED("preconfirmed"),
  CONFIRMED("confirmed"),
  SENT("sent"),
  REJECTED("rejected"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }
    private val BY_NAME = entries.associateBy { it.name }

    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): DisbursementResolution = BY_VALUE[value] ?: BY_NAME[value] ?: throw IllegalArgumentException("Unknown DisbursementResolution: $value")

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

@Converter(autoApply = true)
class DisbursementResolutionConverter : AttributeConverter<DisbursementResolution, String> {
  override fun convertToDatabaseColumn(attribute: DisbursementResolution?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): DisbursementResolution? = dbData?.let { DisbursementResolution.fromValue(it) }
}

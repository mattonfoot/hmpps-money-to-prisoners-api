package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class CreditResolution(@JsonValue val value: String) {
  INITIAL("initial"),
  PENDING("pending"),
  MANUAL("manual"),
  CREDITED("credited"),
  REFUNDED("refunded"),
  FAILED("failed"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }
    private val BY_NAME = entries.associateBy { it.name }

    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): CreditResolution = BY_VALUE[value] ?: BY_NAME[value] ?: throw IllegalArgumentException("Unknown CreditResolution: $value")

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

@Converter(autoApply = true)
class CreditResolutionConverter : AttributeConverter<CreditResolution, String> {
  override fun convertToDatabaseColumn(attribute: CreditResolution?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): CreditResolution? = dbData?.let { CreditResolution.fromValue(it) }
}

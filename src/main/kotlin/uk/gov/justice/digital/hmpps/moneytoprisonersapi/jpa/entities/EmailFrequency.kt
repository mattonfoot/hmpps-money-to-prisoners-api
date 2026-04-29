package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class EmailFrequency(@JsonValue val value: String) {
  NEVER("never"),
  DAILY("daily"),
  WEEKLY("weekly"),
  MONTHLY("monthly"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }
    private val BY_NAME = entries.associateBy { it.name }

    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): EmailFrequency = BY_VALUE[value] ?: BY_NAME[value] ?: throw IllegalArgumentException("Unknown EmailFrequency: $value")
  }
}

@Converter(autoApply = true)
class EmailFrequencyConverter : AttributeConverter<EmailFrequency, String> {
  override fun convertToDatabaseColumn(attribute: EmailFrequency?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): EmailFrequency? = dbData?.let { EmailFrequency.fromValue(it) }
}

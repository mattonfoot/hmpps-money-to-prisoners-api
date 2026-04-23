package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class EmailFrequency(val value: String) {
  DAILY("daily"),
  NEVER("never"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }

    fun fromValue(value: String): EmailFrequency =
      BY_VALUE[value] ?: throw IllegalArgumentException("Unknown EmailFrequency: $value")
  }
}

@Converter(autoApply = true)
class EmailFrequencyConverter : AttributeConverter<EmailFrequency, String> {
  override fun convertToDatabaseColumn(attribute: EmailFrequency?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): EmailFrequency? = dbData?.let { EmailFrequency.fromValue(it) }
}

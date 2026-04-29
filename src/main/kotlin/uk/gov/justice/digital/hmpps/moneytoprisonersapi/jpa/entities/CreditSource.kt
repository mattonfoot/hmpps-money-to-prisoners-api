package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class CreditSource(@JsonValue val value: String) {
  BANK_TRANSFER("bank_transfer"),
  ONLINE("online"),
  UNKNOWN("unknown"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }
    private val BY_NAME = entries.associateBy { it.name }

    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): CreditSource = BY_VALUE[value] ?: BY_NAME[value] ?: throw IllegalArgumentException("Unknown CreditSource: $value")
  }
}

@Converter(autoApply = true)
class CreditSourceConverter : AttributeConverter<CreditSource, String> {
  override fun convertToDatabaseColumn(attribute: CreditSource?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): CreditSource? = dbData?.let { CreditSource.fromValue(it) }
}

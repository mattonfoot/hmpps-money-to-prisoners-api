package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class TransactionSource(@JsonValue val value: String) {
  BANK_TRANSFER("bank_transfer"),
  ADMINISTRATIVE("administrative"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }
    private val BY_NAME = entries.associateBy { it.name }

    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): TransactionSource = BY_VALUE[value] ?: BY_NAME[value] ?: throw IllegalArgumentException("Unknown TransactionSource: $value")
  }
}

@Converter(autoApply = true)
class TransactionSourceConverter : AttributeConverter<TransactionSource, String> {
  override fun convertToDatabaseColumn(attribute: TransactionSource?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): TransactionSource? = dbData?.let { TransactionSource.fromValue(it) }
}

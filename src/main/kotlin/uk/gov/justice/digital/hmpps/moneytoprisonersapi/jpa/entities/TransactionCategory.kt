package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class TransactionCategory(@JsonValue val value: String) {
  CREDIT("credit"),
  DEBIT("debit"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }
    private val BY_NAME = entries.associateBy { it.name }

    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): TransactionCategory =
      BY_VALUE[value] ?: BY_NAME[value] ?: throw IllegalArgumentException("Unknown TransactionCategory: $value")
  }
}

@Converter(autoApply = true)
class TransactionCategoryConverter : AttributeConverter<TransactionCategory, String> {
  override fun convertToDatabaseColumn(attribute: TransactionCategory?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): TransactionCategory? = dbData?.let { TransactionCategory.fromValue(it) }
}

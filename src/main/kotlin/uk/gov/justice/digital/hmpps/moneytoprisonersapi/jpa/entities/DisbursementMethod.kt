package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class DisbursementMethod(val value: String) {
  BANK_TRANSFER("bank_transfer"),
  CHEQUE("cheque"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }

    fun fromValue(value: String): DisbursementMethod =
      BY_VALUE[value] ?: throw IllegalArgumentException("Unknown DisbursementMethod: $value")
  }
}

@Converter(autoApply = true)
class DisbursementMethodConverter : AttributeConverter<DisbursementMethod, String> {
  override fun convertToDatabaseColumn(attribute: DisbursementMethod?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): DisbursementMethod? = dbData?.let { DisbursementMethod.fromValue(it) }
}

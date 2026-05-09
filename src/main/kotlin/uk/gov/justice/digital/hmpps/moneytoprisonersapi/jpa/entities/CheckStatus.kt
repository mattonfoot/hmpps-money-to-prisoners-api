package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
enum class CheckStatus(@JsonValue val value: String) {
  PENDING("pending"),
  ACCEPTED("accepted"),
  REJECTED("rejected"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }
    private val BY_NAME = entries.associateBy { it.name }

    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): CheckStatus = BY_VALUE[value] ?: BY_NAME[value] ?: throw IllegalArgumentException("Unknown CheckStatus: $value")
  }
}

@Converter(autoApply = true)
class CheckStatusConverter : AttributeConverter<CheckStatus, String> {
  override fun convertToDatabaseColumn(attribute: CheckStatus?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): CheckStatus? = dbData?.let { CheckStatus.fromValue(it) }
}

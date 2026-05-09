package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
enum class LogAction(@JsonValue val value: String) {
  CREATED("created"),
  CREDITED("credited"),
  REFUNDED("refunded"),
  RECONCILED("reconciled"),
  REVIEWED("reviewed"),
  MANUAL("manual"),
  FAILED("failed"),
  EDITED("edited"),
  REJECTED("rejected"),
  CONFIRMED("confirmed"),
  SENT("sent"),
  PRECONFIRMED("preconfirmed"),
  ;

  companion object {
    private val BY_VALUE = entries.associateBy { it.value }
    private val BY_NAME = entries.associateBy { it.name }

    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): LogAction = BY_VALUE[value] ?: BY_NAME[value] ?: throw IllegalArgumentException("Unknown LogAction: $value")
  }
}

@Converter(autoApply = true)
class LogActionConverter : AttributeConverter<LogAction, String> {
  override fun convertToDatabaseColumn(attribute: LogAction?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): LogAction? = dbData?.let { LogAction.fromValue(it) }
}

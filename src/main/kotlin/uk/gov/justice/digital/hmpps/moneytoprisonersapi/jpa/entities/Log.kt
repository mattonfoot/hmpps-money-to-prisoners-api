package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class LogAction(val value: String) {
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

    fun fromValue(value: String): LogAction =
      BY_VALUE[value] ?: throw IllegalArgumentException("Unknown LogAction: $value")
  }
}

@Converter(autoApply = true)
class LogActionConverter : AttributeConverter<LogAction, String> {
  override fun convertToDatabaseColumn(attribute: LogAction?): String? = attribute?.value
  override fun convertToEntityAttribute(dbData: String?): LogAction? = dbData?.let { LogAction.fromValue(it) }
}

@Entity
@Table(name = "credit_log")
class Log(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "log_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(nullable = false, length = 50)
  val action: LogAction,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credit_id")
  var credit: Credit? = null,

  @Column(name = "user_id")
  var userId: String? = null,

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,
) {

  @PrePersist
  fun onCreate() {
    if (created == null) {
      created = LocalDateTime.now()
    }
  }
}

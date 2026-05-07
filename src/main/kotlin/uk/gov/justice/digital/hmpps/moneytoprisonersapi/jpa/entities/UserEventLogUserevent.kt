package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(
  name = "user_event_log_userevent",
  schema = "public",
  indexes = [
    Index(
      name = "user_event_log_userevent_timestamp_f09e88ba",
      columnList = "timestamp",
    ),
    Index(
      name = "user_event_log_userevent_api_url_path_f23880c4",
      columnList = "api_url_path",
    ),
    Index(
      name = "user_event_log_userevent_api_url_path_f23880c4_like",
      columnList = "api_url_path",
    ),
    Index(
      name = "user_event_log_userevent_user_id_69279745",
      columnList = "user_id",
    ),
  ],
)
open class UserEventLogUserevent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long = 0L

  @NotNull
  @Column(name = "\"timestamp\"", nullable = false)
  open var timestamp: OffsetDateTime = OffsetDateTime.now()

  @Size(max = 254)
  @NotNull
  @Column(name = "kind", nullable = false, length = 254)
  open var kind: String = ""

  @Size(max = 5000)
  @NotNull
  @Column(name = "api_url_path", nullable = false, length = 5000)
  open var apiUrlPath: String = ""

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data")
  open var data: Map<String, Any>? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}

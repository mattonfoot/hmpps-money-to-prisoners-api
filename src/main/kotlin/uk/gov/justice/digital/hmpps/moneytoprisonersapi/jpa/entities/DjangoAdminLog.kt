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
import java.time.OffsetDateTime

@Entity
@Table(
  name = "django_admin_log",
  schema = "public",
  indexes = [
    Index(
      name = "django_admin_log_content_type_id_c4bce8eb",
      columnList = "content_type_id",
    ),
    Index(
      name = "django_admin_log_user_id_c564eba6",
      columnList = "user_id",
    ),
  ],
)
open class DjangoAdminLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @Column(name = "action_time", nullable = false)
  open var actionTime: OffsetDateTime = OffsetDateTime.now()

  @Column(name = "object_id", length = Integer.MAX_VALUE)
  open var objectId: String? = null

  @Size(max = 200)
  @NotNull
  @Column(name = "object_repr", nullable = false, length = 200)
  open var objectRepr: String = ""

  @NotNull
  @Column(name = "action_flag", nullable = false)
  open var actionFlag: Short = 0

  @NotNull
  @Column(name = "change_message", nullable = false, length = Integer.MAX_VALUE)
  open var changeMessage: String = ""

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_type_id")
  open var contentType: DjangoContentType? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}

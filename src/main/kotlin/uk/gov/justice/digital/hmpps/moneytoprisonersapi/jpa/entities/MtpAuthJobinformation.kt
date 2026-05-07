package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "mtp_auth_jobinformation",
  schema = "public",
  uniqueConstraints = [
    UniqueConstraint(
      name = "mtp_auth_jobinformation_user_id_key",
      columnNames = ["user_id"],
    ),
  ],
)
open class MtpAuthJobinformation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "modified", nullable = false)
  open var modified: OffsetDateTime = OffsetDateTime.now()

  @Size(max = 255)
  @NotNull
  @Column(name = "title", nullable = false)
  open var title: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "prison_estate", nullable = false)
  open var prisonEstate: String = ""

  @NotNull
  @Column(name = "tasks", nullable = false, length = Integer.MAX_VALUE)
  open var tasks: String = ""

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}

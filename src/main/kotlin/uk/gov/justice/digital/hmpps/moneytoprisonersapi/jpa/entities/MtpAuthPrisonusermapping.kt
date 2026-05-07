package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

@Entity
@Table(
  name = "mtp_auth_prisonusermapping",
  schema = "public",
  uniqueConstraints = [
    UniqueConstraint(
      name = "mtp_auth_prisonusermapping_user_id_key",
      columnNames = ["user_id"],
    ),
  ],
)
open class MtpAuthPrisonusermapping {
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

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "mtp_auth_prisonusermapping_prisons",
    joinColumns = [JoinColumn(name = "prisonusermapping_id")],
    inverseJoinColumns = [JoinColumn(name = "prison_id")],
  )
  open var prisons: MutableSet<PrisonPrison> = mutableSetOf()
}

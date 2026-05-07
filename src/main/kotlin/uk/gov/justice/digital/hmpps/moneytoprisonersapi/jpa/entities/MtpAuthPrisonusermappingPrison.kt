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
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull

@Entity
@Table(
  name = "mtp_auth_prisonusermapping_prisons",
  schema = "public",
  indexes = [
    Index(
      name = "mtp_auth_prisonusermapping_prisonusermapping_id_88834497",
      columnList = "prisonusermapping_id",
    ),
    Index(
      name = "mtp_auth_prisonusermapping_prisons_prison_id_3ca6e6ba",
      columnList = "prison_id",
    ),
    Index(
      name = "mtp_auth_prisonusermapping_prisons_prison_id_3ca6e6ba_like",
      columnList = "prison_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "mtp_auth_prisonusermappi_prisonusermapping_id_pri_8c361331_uniq",
      columnNames = [
        "prisonusermapping_id",
        "prison_id",
      ],
    ),
  ],
)
open class MtpAuthPrisonusermappingPrison {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prisonusermapping_id", nullable = false)
  open var prisonusermapping: MtpAuthPrisonusermapping? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null
}

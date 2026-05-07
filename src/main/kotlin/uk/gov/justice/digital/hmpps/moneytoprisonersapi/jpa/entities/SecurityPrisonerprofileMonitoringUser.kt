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
  name = "security_prisonerprofile_monitoring_users",
  schema = "public",
  indexes = [
    Index(
      name = "security_prisonerprofile_m_prisonerprofile_id_2461f9ff",
      columnList = "prisonerprofile_id",
    ),
    Index(
      name = "security_prisonerprofile_monitoring_users_user_id_efb5a596",
      columnList = "user_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_prisonerprofile_prisonerprofile_id_user__074b4ba0_uniq",
      columnNames = [
        "prisonerprofile_id",
        "user_id",
      ],
    ),
  ],
)
open class SecurityPrisonerprofileMonitoringUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prisonerprofile_id", nullable = false)
  open var prisonerprofile: SecurityPrisonerprofile? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}

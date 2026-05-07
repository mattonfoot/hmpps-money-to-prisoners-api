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
  name = "mtp_auth_accountrequest",
  schema = "public",
  indexes = [
    Index(
      name = "mtp_auth_accountrequest_prison_id_db94a5e0",
      columnList = "prison_id",
    ),
    Index(
      name = "mtp_auth_accountrequest_prison_id_db94a5e0_like",
      columnList = "prison_id",
    ),
    Index(
      name = "mtp_auth_accountrequest_role_id_59c6d292",
      columnList = "role_id",
    ),
  ],
)
open class MtpAuthAccountrequest {
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

  @Size(max = 150)
  @NotNull
  @Column(name = "username", nullable = false, length = 150)
  open var username: String = ""

  @Size(max = 30)
  @NotNull
  @Column(name = "first_name", nullable = false, length = 30)
  open var firstName: String = ""

  @Size(max = 30)
  @NotNull
  @Column(name = "last_name", nullable = false, length = 30)
  open var lastName: String = ""

  @Size(max = 254)
  @NotNull
  @Column(name = "email", nullable = false, length = 254)
  open var email: String = ""

  @NotNull
  @Column(name = "reason", nullable = false, length = Integer.MAX_VALUE)
  open var reason: String = ""

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prison_id")
  open var prison: PrisonPrison? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  open var role: MtpAuthRole? = null

  @Size(max = 254)
  @Column(name = "manager_email", length = 254)
  open var managerEmail: String? = null
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_senderprofile",
  schema = "public",
  indexes = [
    Index(
      name = "security_se_credit__fcd1fc_idx",
      columnList = "credit_count",
    ),
    Index(
      name = "security_se_credit__cd9fdb_idx",
      columnList = "credit_total",
    ),
  ],
)
open class SecuritySenderprofile {
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
  @Column(name = "credit_count", nullable = false)
  open var creditCount: Long = 0L

  @NotNull
  @Column(name = "credit_total", nullable = false)
  open var creditTotal: Long = 0L

  // Manually-maintained back-references.
  @OneToMany(mappedBy = "senderProfile", fetch = FetchType.LAZY)
  open var credits: MutableList<CreditCredit> = mutableListOf()

  // Django has no `sender_profile_monitoring_users` table — that data lives on
  // `security_debitcardsenderdetails_monitoring_users` etc. (per-detail). Stub
  // for now until call sites are redesigned to walk the detail children.
  @Transient
  open var monitoringUsers: MutableSet<Int> = mutableSetOf()

  @Transient
  open var prisons: MutableSet<PrisonPrison> = mutableSetOf()
}

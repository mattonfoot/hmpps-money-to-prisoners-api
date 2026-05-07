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
  name = "security_recipientprofile",
  schema = "public",
  indexes = [
    Index(
      name = "security_re_disburs_1ea4fb_idx",
      columnList = "disbursement_count",
    ),
    Index(
      name = "security_re_disburs_478726_idx",
      columnList = "disbursement_total",
    ),
  ],
)
open class SecurityRecipientprofile {
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
  @Column(name = "disbursement_count", nullable = false)
  open var disbursementCount: Long = 0L

  @NotNull
  @Column(name = "disbursement_total", nullable = false)
  open var disbursementTotal: Long = 0L

  @OneToMany(mappedBy = "recipientProfile", fetch = FetchType.LAZY)
  open var disbursements: MutableList<DisbursementDisbursement> = mutableListOf()

  // Django models monitoringUsers on detail children, not on the recipient
  // profile itself. Stub for now — call sites that need this require redesign.
  @Transient
  open var monitoringUsers: MutableSet<Int> = mutableSetOf()

  @Transient
  open var prisons: MutableSet<PrisonPrison> = mutableSetOf()
}

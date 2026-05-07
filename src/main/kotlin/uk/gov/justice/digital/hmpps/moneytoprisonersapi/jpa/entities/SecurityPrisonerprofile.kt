package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_prisonerprofile",
  schema = "public",
  indexes = [
    Index(
      name = "security_prisonerprofile_prisoner_number_69c46ca5",
      columnList = "prisoner_number",
    ),
    Index(
      name = "security_prisonerprofile_prisoner_number_69c46ca5_like",
      columnList = "prisoner_number",
    ),
    Index(
      name = "security_pr_credit__cad7c2_idx",
      columnList = "credit_count",
    ),
    Index(
      name = "security_pr_credit__71ade5_idx",
      columnList = "credit_total",
    ),
    Index(
      name = "security_prisonerprofile_current_prison_id_3cd5104d",
      columnList = "current_prison_id",
    ),
    Index(
      name = "security_prisonerprofile_current_prison_id_3cd5104d_like",
      columnList = "current_prison_id",
    ),
    Index(
      name = "security_pr_disburs_26653f_idx",
      columnList = "disbursement_count",
    ),
    Index(
      name = "security_pr_disburs_74948c_idx",
      columnList = "disbursement_total",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_prisonerprofile_prisoner_number_prisoner_56305d86_uniq",
      columnNames = [
        "prisoner_number",
        "prisoner_dob",
      ],
    ),
  ],
)
open class SecurityPrisonerprofile {
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

  @Size(max = 250)
  @NotNull
  @Column(name = "prisoner_name", nullable = false, length = 250)
  open var prisonerName: String = ""

  @Size(max = 250)
  @NotNull
  @Column(name = "prisoner_number", nullable = false, length = 250)
  open var prisonerNumber: String = ""

  @Column(name = "prisoner_dob")
  open var prisonerDob: LocalDate? = null

  @NotNull
  @Column(name = "credit_count", nullable = false)
  open var creditCount: Long = 0L

  @NotNull
  @Column(name = "credit_total", nullable = false)
  open var creditTotal: Long = 0L

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_prison_id")
  open var currentPrison: PrisonPrison? = null

  @NotNull
  @Column(name = "disbursement_count", nullable = false)
  open var disbursementCount: Long = 0L

  @NotNull
  @Column(name = "disbursement_total", nullable = false)
  open var disbursementTotal: Long = 0L

  @OneToMany(mappedBy = "prisonerProfile", fetch = FetchType.LAZY)
  open var credits: MutableList<CreditCredit> = mutableListOf()

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "security_prisonerprofile_monitoring_users",
    joinColumns = [JoinColumn(name = "prisonerprofile_id")],
  )
  @Column(name = "user_id")
  open var monitoringUsers: MutableSet<Int> = mutableSetOf()

  // Django splits prisons across `security_prisonerprofile_prisons` join.
  // Stub for now until call sites are updated.
  @Transient
  open var prisons: MutableSet<PrisonPrison> = mutableSetOf()
}

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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_check",
  schema = "public",
  indexes = [
    Index(
      name = "security_check_status_65d97f63",
      columnList = "status",
    ),
    Index(
      name = "security_check_status_65d97f63_like",
      columnList = "status",
    ),
    Index(
      name = "security_check_actioned_by_id_4cf02638",
      columnList = "actioned_by_id",
    ),
    Index(
      name = "security_check_assigned_to_id_2eedaae6",
      columnList = "assigned_to_id",
    ),
    Index(
      name = "security_check_auto_accept_rule_state_id_0cb96cd1",
      columnList = "auto_accept_rule_state_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_check_credit_id_key",
      columnNames = ["credit_id"],
    ),
  ],
)
open class SecurityCheck {
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

  @Size(max = 50)
  @NotNull
  @Column(name = "status", nullable = false, length = 50)
  open var status: String = ""

  @Column(name = "rules", columnDefinition = "varchar [](50)")
  open var rules: Any? = null

  @Column(name = "actioned_at")
  open var actionedAt: OffsetDateTime? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actioned_by_id")
  open var actionedBy: AuthUser? = null

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "credit_id", nullable = false)
  open var credit: CreditCredit? = null

  @NotNull
  @Column(name = "decision_reason", nullable = false, length = Integer.MAX_VALUE)
  open var decisionReason: String = ""

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to_id")
  open var assignedTo: AuthUser? = null

  @Column(name = "description", columnDefinition = "varchar [](200)")
  open var description: Any? = null

  @NotNull
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "rejection_reasons", nullable = false)
  open var rejectionReasons: Map<String, Any>? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "auto_accept_rule_state_id")
  open var autoAcceptRuleState: SecurityCheckautoacceptrulestate? =
    null
}

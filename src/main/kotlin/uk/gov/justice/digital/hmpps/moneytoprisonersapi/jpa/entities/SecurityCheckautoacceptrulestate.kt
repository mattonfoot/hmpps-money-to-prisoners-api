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
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_checkautoacceptrulestate",
  schema = "public",
  indexes = [
    Index(
      name = "security_checkautoacceptrulestate_added_by_id_4e780e41",
      columnList = "added_by_id",
    ),
    Index(
      name = "security_checkautoacceptrulestate_auto_accept_rule_id_0e14d501",
      columnList = "auto_accept_rule_id",
    ),
  ],
)
open class SecurityCheckautoacceptrulestate {
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
  @Column(name = "active", nullable = false)
  open var active: Boolean = false

  @NotNull
  @Column(name = "reason", nullable = false, length = Integer.MAX_VALUE)
  open var reason: String = ""

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "added_by_id")
  open var addedBy: AuthUser? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "auto_accept_rule_id", nullable = false)
  open var autoAcceptRule: SecurityCheckautoacceptrule? = null
}

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
  name = "security_bankaccount_monitoring_users",
  schema = "public",
  indexes = [
    Index(
      name = "security_bankaccount_monitoring_users_bankaccount_id_bda33b69",
      columnList = "bankaccount_id",
    ),
    Index(
      name = "security_bankaccount_monitoring_users_user_id_d14884ce",
      columnList = "user_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_bankaccount_mon_bankaccount_id_user_id_47e949ef_uniq",
      columnNames = [
        "bankaccount_id",
        "user_id",
      ],
    ),
  ],
)
open class SecurityBankaccountMonitoringUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "bankaccount_id", nullable = false)
  open var bankaccount: SecurityBankaccount? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}

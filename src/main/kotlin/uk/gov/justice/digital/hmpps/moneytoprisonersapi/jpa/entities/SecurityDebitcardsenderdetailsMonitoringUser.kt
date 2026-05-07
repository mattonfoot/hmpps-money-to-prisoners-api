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
  name = "security_debitcardsenderdetails_monitoring_users",
  schema = "public",
  indexes = [
    Index(
      name = "security_debitcardsenderde_debitcardsenderdetails_id_bad03fe6",
      columnList = "debitcardsenderdetails_id",
    ),
    Index(
      name = "security_debitcardsenderde_user_id_6e81d3cd",
      columnList = "user_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_debitcardsender_debitcardsenderdetails_i_636662c6_uniq",
      columnNames = [
        "debitcardsenderdetails_id",
        "user_id",
      ],
    ),
  ],
)
open class SecurityDebitcardsenderdetailsMonitoringUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "debitcardsenderdetails_id", nullable = false)
  open var debitcardsenderdetails: SecurityDebitcardsenderdetail? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}

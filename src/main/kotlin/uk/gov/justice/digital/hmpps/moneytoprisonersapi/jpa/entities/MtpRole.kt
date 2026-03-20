package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "mtp_roles")
class MtpRole(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(nullable = false, unique = true, length = 150)
  val name: String,

  @Column(name = "key_group", nullable = false, length = 150)
  val keyGroup: String = "",

  /** Comma-separated list of additional group names */
  @Column(name = "other_groups", nullable = false, columnDefinition = "TEXT")
  val otherGroups: String = "",

  /** Application identifier, e.g. "cashbook", "noms-ops", "bank-admin", "send-money" */
  @Column(nullable = false, length = 50)
  val application: String = "",
)

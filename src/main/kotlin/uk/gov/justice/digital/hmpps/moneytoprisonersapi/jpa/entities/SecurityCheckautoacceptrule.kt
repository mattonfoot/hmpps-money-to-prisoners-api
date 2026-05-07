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
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_checkautoacceptrule",
  schema = "public",
  indexes = [
    Index(
      name = "security_checkautoacceptru_debit_card_sender_details__f5a21a6e",
      columnList = "debit_card_sender_details_id",
    ),
    Index(
      name = "security_checkautoacceptrule_prisoner_profile_id_f26c6f9f",
      columnList = "prisoner_profile_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_checkautoaccept_debit_card_sender_detail_b94e44a7_uniq",
      columnNames = [
        "debit_card_sender_details_id",
        "prisoner_profile_id",
      ],
    ),
  ],
)
open class SecurityCheckautoacceptrule {
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
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "debit_card_sender_details_id", nullable = false)
  open var debitCardSenderDetails: SecurityDebitcardsenderdetail? =
    null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prisoner_profile_id", nullable = false)
  open var prisonerProfile: SecurityPrisonerprofile? =
    null
}

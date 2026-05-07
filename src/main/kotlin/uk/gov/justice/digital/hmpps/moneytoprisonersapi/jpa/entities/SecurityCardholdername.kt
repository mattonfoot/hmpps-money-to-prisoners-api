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

@Entity
@Table(
  name = "security_cardholdername",
  schema = "public",
  indexes = [
    Index(
      name = "security_cardholdername_debit_card_sender_details_id_6b7d84bf",
      columnList = "debit_card_sender_details_id",
    ),
  ],
)
open class SecurityCardholdername {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 250)
  @NotNull
  @Column(name = "name", nullable = false, length = 250)
  open var name: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "debit_card_sender_details_id", nullable = false)
  open var debitCardSenderDetails: SecurityDebitcardsenderdetail? =
    null
}

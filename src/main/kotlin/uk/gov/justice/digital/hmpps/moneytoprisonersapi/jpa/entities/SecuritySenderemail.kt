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
  name = "security_senderemail",
  schema = "public",
  indexes = [
    Index(
      name = "security_senderemail_debit_card_sender_details_id_ec1ee3c2",
      columnList = "debit_card_sender_details_id",
    ),
  ],
)
open class SecuritySenderemail {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 250)
  @NotNull
  @Column(name = "email", nullable = false, length = 250)
  open var email: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "debit_card_sender_details_id", nullable = false)
  open var debitCardSenderDetails: SecurityDebitcardsenderdetail? = null
}

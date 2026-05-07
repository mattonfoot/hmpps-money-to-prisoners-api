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
import jakarta.validation.constraints.Size

@Entity
@Table(
  name = "payment_billingaddress",
  schema = "public",
  indexes = [
    Index(
      name = "payment_billingaddress_debit_card_sender_details_id_20dab45c",
      columnList = "debit_card_sender_details_id",
    ),
  ],
)
open class PaymentBillingaddress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 250)
  @Column(name = "line1", length = 250)
  open var line1: String? = null

  @Size(max = 250)
  @Column(name = "line2", length = 250)
  open var line2: String? = null

  @Size(max = 250)
  @Column(name = "city", length = 250)
  open var city: String? = null

  @Size(max = 250)
  @Column(name = "country", length = 250)
  open var country: String? = null

  @Size(max = 250)
  @Column(name = "postcode", length = 250)
  open var postcode: String? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "debit_card_sender_details_id")
  open var debitCardSenderDetails: SecurityDebitcardsenderdetail? =
    null
}

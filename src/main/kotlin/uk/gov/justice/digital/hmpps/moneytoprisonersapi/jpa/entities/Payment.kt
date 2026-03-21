package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "payment_payment")
class Payment(
  @Id
  @Column(name = "uuid", columnDefinition = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(nullable = false)
  val amount: Long = 0,

  @Column(name = "service_charge", nullable = false)
  var serviceCharge: Long = 0,

  @Column(length = 50)
  var status: String? = null,

  @Column(name = "processor_id", length = 250)
  var processorId: String? = null,

  @Column(name = "recipient_name", length = 250)
  var recipientName: String? = null,

  @Column(length = 254)
  var email: String? = null,

  @Column(name = "cardholder_name", length = 250)
  var cardholderName: String? = null,

  @Column(name = "card_number_first_digits", length = 6)
  var cardNumberFirstDigits: String? = null,

  @Column(name = "card_number_last_digits", length = 4)
  var cardNumberLastDigits: String? = null,

  @Column(name = "card_expiry_date", length = 5)
  var cardExpiryDate: String? = null,

  @Column(name = "card_brand", length = 250)
  var cardBrand: String? = null,

  @Column(name = "ip_address", length = 45)
  var ipAddress: String? = null,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credit_id", nullable = false, unique = true)
  var credit: Credit? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "billing_address_id")
  var billingAddress: BillingAddress? = null,

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,

  @Column(nullable = false)
  var modified: LocalDateTime? = null,
) {

  @PrePersist
  fun onCreate() {
    val now = LocalDateTime.now()
    created = now
    modified = now
  }

  @PreUpdate
  fun onUpdate() {
    modified = LocalDateTime.now()
  }
}

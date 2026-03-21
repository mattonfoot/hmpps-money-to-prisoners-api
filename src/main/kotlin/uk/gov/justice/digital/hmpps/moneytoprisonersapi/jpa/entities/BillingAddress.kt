package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "payment_billingaddress")
class BillingAddress(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "billing_address_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(length = 250)
  var line1: String? = null,

  @Column(length = 250)
  var line2: String? = null,

  @Column(length = 250)
  var city: String? = null,

  @Column(length = 250)
  var country: String? = null,

  @Column(length = 250)
  var postcode: String? = null,

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

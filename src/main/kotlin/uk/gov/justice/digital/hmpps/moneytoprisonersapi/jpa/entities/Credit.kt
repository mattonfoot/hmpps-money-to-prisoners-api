package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
  name = "credits",
  indexes = [
    Index(name = "idx_credits_prisoner_number", columnList = "prisoner_number"),
    Index(name = "idx_credits_amount", columnList = "amount"),
    Index(name = "idx_credits_received_at", columnList = "received_at"),
    Index(name = "idx_credits_resolution", columnList = "resolution"),
    Index(name = "idx_credits_owner", columnList = "owner"),
  ],
)
class Credit(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "credit_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(nullable = false)
  val amount: Long,

  @Column(name = "prisoner_number", length = 250)
  var prisonerNumber: String? = null,

  @Column(name = "prisoner_name", length = 250)
  var prisonerName: String? = null,

  @Column(name = "prisoner_dob")
  var prisonerDob: LocalDate? = null,

  @Column(length = 10)
  var prison: String? = null,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var resolution: CreditResolution = CreditResolution.PENDING,

  @Column(nullable = false)
  var reconciled: Boolean = false,

  @Column(nullable = false)
  var reviewed: Boolean = false,

  @Column(nullable = false)
  var blocked: Boolean = false,

  @Column(name = "incomplete_sender_info", nullable = false)
  var incompleteSenderInfo: Boolean = false,

  @Column(name = "received_at")
  var receivedAt: LocalDateTime? = null,

  @Column
  var owner: String? = null,

  @Column(name = "nomis_transaction_id", length = 50)
  var nomisTransactionId: String? = null,

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,

  @Column(nullable = false)
  var modified: LocalDateTime? = null,
) {

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  var source: CreditSource = CreditSource.UNKNOWN

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

  fun transitionResolution(newResolution: CreditResolution) {
    if (!CreditResolution.isValidTransition(resolution, newResolution)) {
      throw InvalidCreditStateException(resolution, newResolution)
    }
    resolution = newResolution
  }

  override fun toString(): String {
    val pounds = amount / 100
    val pence = (amount % 100).let { if (it < 0) -it else it }
    return "Credit($prisonerNumber, £$pounds.${pence.toString().padStart(2, '0')}, $resolution)"
  }
}

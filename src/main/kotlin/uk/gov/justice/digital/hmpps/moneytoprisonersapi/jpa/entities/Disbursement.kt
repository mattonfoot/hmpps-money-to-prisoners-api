package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
  name = "disbursement_disbursement",
  indexes = [
    Index(name = "idx_disbursements_prisoner_number", columnList = "prisoner_number"),
    Index(name = "idx_disbursements_resolution", columnList = "resolution"),
    Index(name = "idx_disbursements_prison", columnList = "prison"),
  ],
)
class Disbursement(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "disbursement_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(nullable = false)
  var amount: Long,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var method: DisbursementMethod,

  @Column(length = 10)
  var prison: String? = null,

  @Column(name = "prisoner_number", length = 250)
  var prisonerNumber: String? = null,

  @Column(name = "prisoner_name", length = 250)
  var prisonerName: String? = null,

  @Column(name = "recipient_first_name", length = 250)
  var recipientFirstName: String? = null,

  @Column(name = "recipient_last_name", length = 250)
  var recipientLastName: String? = null,

  @Column(name = "recipient_email", length = 254)
  var recipientEmail: String? = null,

  @Column(name = "address_line1", length = 250)
  var addressLine1: String? = null,

  @Column(name = "address_line2", length = 250)
  var addressLine2: String? = null,

  @Column(name = "city", length = 250)
  var city: String? = null,

  @Column(name = "postcode", length = 250)
  var postcode: String? = null,

  @Column(name = "country", length = 250)
  var country: String? = null,

  @Column(name = "sort_code", length = 50)
  var sortCode: String? = null,

  @Column(name = "account_number", length = 50)
  var accountNumber: String? = null,

  @Column(name = "roll_number", length = 50)
  var rollNumber: String? = null,

  @Column(name = "recipient_is_company", nullable = false)
  var recipientIsCompany: Boolean = false,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var resolution: DisbursementResolution = DisbursementResolution.PENDING,

  @Column(name = "nomis_transaction_id", length = 50)
  var nomisTransactionId: String? = null,

  @Column(name = "invoice_number", length = 50)
  var invoiceNumber: String? = null,

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,

  @Column(nullable = false)
  var modified: LocalDateTime? = null,
) {

  @OneToMany(mappedBy = "disbursement", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  var logs: MutableList<DisbursementLog> = mutableListOf()

  @OneToMany(mappedBy = "disbursement", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  var comments: MutableList<DisbursementComment> = mutableListOf()

  val recipientName: String?
    get() = if (recipientIsCompany) {
      recipientFirstName
    } else {
      listOfNotNull(recipientFirstName, recipientLastName).joinToString(" ").ifBlank { null }
    }

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

  fun transitionResolution(newResolution: DisbursementResolution) {
    if (resolution == newResolution) return // idempotent
    if (!DisbursementResolution.isValidTransition(resolution, newResolution)) {
      throw InvalidDisbursementStateException(resolution, newResolution)
    }
    resolution = newResolution
  }

  override fun toString(): String {
    val pounds = amount / 100
    val pence = (amount % 100).let { if (it < 0) -it else it }
    return "Disbursement($prisonerNumber, £$pounds.${pence.toString().padStart(2, '0')}, $resolution)"
  }
}

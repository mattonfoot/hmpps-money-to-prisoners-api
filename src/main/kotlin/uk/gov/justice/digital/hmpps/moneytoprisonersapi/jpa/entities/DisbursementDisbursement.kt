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
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "disbursement_disbursement",
  schema = "public",
  indexes = [
    Index(
      name = "disbursemen_created_42f222_idx",
      columnList = "created, id",
    ),
    Index(
      name = "disbursemen_created_7ec31c_idx",
      columnList = "created, id",
    ),
    Index(
      name = "disbursemen_amount_6ab60e_idx",
      columnList = "amount, id",
    ),
    Index(
      name = "disbursemen_amount_8f59bd_idx",
      columnList = "amount, id",
    ),
    Index(
      name = "disbursement_disbursement_amount_eb51ccce",
      columnList = "amount",
    ),
    Index(
      name = "disbursemen_prisone_11c788_idx",
      columnList = "prisoner_number, id",
    ),
    Index(
      name = "disbursemen_prisone_fbc552_idx",
      columnList = "prisoner_number, id",
    ),
    Index(
      name = "disbursement_disbursement_prisoner_number_c2bf69f0",
      columnList = "prisoner_number",
    ),
    Index(
      name = "disbursement_disbursement_prisoner_number_c2bf69f0_like",
      columnList = "prisoner_number",
    ),
    Index(
      name = "disbursement_disbursement_resolution_01be1c05",
      columnList = "resolution",
    ),
    Index(
      name = "disbursement_disbursement_resolution_01be1c05_like",
      columnList = "resolution",
    ),
    Index(
      name = "disbursement_disbursement_method_a227365a",
      columnList = "method",
    ),
    Index(
      name = "disbursement_disbursement_method_a227365a_like",
      columnList = "method",
    ),
    Index(
      name = "disbursement_disbursement_prison_id_5321b0f5",
      columnList = "prison_id",
    ),
    Index(
      name = "disbursement_disbursement_prison_id_5321b0f5_like",
      columnList = "prison_id",
    ),
    Index(
      name = "disbursement_disbursement_prisoner_profile_id_c0840894",
      columnList = "prisoner_profile_id",
    ),
    Index(
      name = "disbursement_disbursement_recipient_profile_id_9da8689c",
      columnList = "recipient_profile_id",
    ),
  ],
)
open class DisbursementDisbursement {
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
  @Column(name = "amount", nullable = false)
  open var amount: Int = 0

  @Size(max = 250)
  @NotNull
  @Column(name = "prisoner_number", nullable = false, length = 250)
  open var prisonerNumber: String = ""

  @Size(max = 50)
  @NotNull
  @Column(name = "resolution", nullable = false, length = 50)
  // Django: `resolution = models.CharField(default=DISBURSEMENT_RESOLUTION.PENDING)`.
  open var resolution: String = "pending"

  @Size(max = 50)
  @NotNull
  @Column(name = "method", nullable = false, length = 50)
  open var method: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null

  @Size(max = 50)
  @Column(name = "account_number", length = 50)
  open var accountNumber: String? = null

  @Size(max = 250)
  @Column(name = "address_line1", length = 250)
  open var addressLine1: String? = null

  @Size(max = 250)
  @Column(name = "address_line2", length = 250)
  open var addressLine2: String? = null

  @Size(max = 250)
  @Column(name = "city", length = 250)
  open var city: String? = null

  @Size(max = 250)
  @Column(name = "country", length = 250)
  open var country: String? = null

  @Size(max = 250)
  @Column(name = "postcode", length = 250)
  open var postcode: String? = null

  @Size(max = 254)
  @Column(name = "recipient_email", length = 254)
  open var recipientEmail: String? = null

  @Size(max = 250)
  @NotNull
  @Column(name = "recipient_first_name", nullable = false, length = 250)
  open var recipientFirstName: String = ""

  @Size(max = 250)
  @NotNull
  @Column(name = "recipient_last_name", nullable = false, length = 250)
  open var recipientLastName: String = ""

  @Size(max = 50)
  @Column(name = "roll_number", length = 50)
  open var rollNumber: String? = null

  @Size(max = 50)
  @Column(name = "sort_code", length = 50)
  open var sortCode: String? = null

  @Size(max = 250)
  @NotNull
  @Column(name = "prisoner_name", nullable = false, length = 250)
  open var prisonerName: String = ""

  @Size(max = 50)
  @Column(name = "nomis_transaction_id", length = 50)
  open var nomisTransactionId: String? = null

  @Size(max = 250)
  @NotNull
  @Column(name = "remittance_description", nullable = false, length = 250)
  open var remittanceDescription: String = ""

  @NotNull
  @Column(name = "recipient_is_company", nullable = false)
  open var recipientIsCompany: Boolean = false

  @Size(max = 50)
  @Column(name = "invoice_number", length = 50)
  open var invoiceNumber: String? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prisoner_profile_id")
  open var prisonerProfile: SecurityPrisonerprofile? =
    null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_profile_id")
  open var recipientProfile: SecurityRecipientprofile? =
    null

  @OneToMany(mappedBy = "disbursement", fetch = FetchType.LAZY)
  open var logs: MutableList<DisbursementLog> = mutableListOf()

  @OneToMany(mappedBy = "disbursement", fetch = FetchType.LAZY)
  open var comments: MutableList<DisbursementComment> = mutableListOf()
}

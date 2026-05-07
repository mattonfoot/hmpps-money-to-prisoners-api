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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
  name = "credit_credit",
  schema = "public",
  indexes = [
    Index(
      name = "credit_cred_created_18d594_idx",
      columnList = "created",
    ),
    Index(
      name = "credit_cred_amount_6de366_idx",
      columnList = "amount, id",
    ),
    Index(
      name = "credit_cred_amount_7277df_idx",
      columnList = "amount, id",
    ),
    Index(
      name = "credit_credit_amount_0868d791",
      columnList = "amount",
    ),
    Index(
      name = "credit_cred_prisone_49e443_idx",
      columnList = "prisoner_number, id",
    ),
    Index(
      name = "credit_cred_prisone_ffb7bb_idx",
      columnList = "prisoner_number, id",
    ),
    Index(
      name = "credit_cred_prisone_d82fa6_idx",
      columnList = "prisoner_number, prisoner_dob",
    ),
    Index(
      name = "credit_credit_prisoner_number_ea35c5f1",
      columnList = "prisoner_number",
    ),
    Index(
      name = "credit_credit_prisoner_number_ea35c5f1_like",
      columnList = "prisoner_number",
    ),
    Index(
      name = "credit_cred_receive_1c4554_idx",
      columnList = "received_at, id",
    ),
    Index(
      name = "credit_cred_receive_920711_idx",
      columnList = "received_at, id",
    ),
    Index(
      name = "credit_credit_received_at_2b80843e",
      columnList = "received_at",
    ),
    Index(
      name = "credit_credit_resolution_0b70738b",
      columnList = "resolution",
    ),
    Index(
      name = "credit_credit_resolution_0b70738b_like",
      columnList = "resolution",
    ),
    Index(
      name = "credit_cred_owner_i_cac17f_idx",
      columnList = "owner_id, reconciled, resolution",
    ),
    Index(
      name = "credit_credit_owner_id_29a97902",
      columnList = "owner_id",
    ),
    Index(
      name = "credit_credit_prison_id_7e918ed9",
      columnList = "prison_id",
    ),
    Index(
      name = "credit_credit_prison_id_7e918ed9_like",
      columnList = "prison_id",
    ),
    Index(
      name = "credit_credit_prisoner_profile_id_7ddd01f3",
      columnList = "prisoner_profile_id",
    ),
    Index(
      name = "credit_credit_sender_profile_id_bec7c2fb",
      columnList = "sender_profile_id",
    ),
    Index(
      name = "credit_credit_private_estate_batch_id_a716b065",
      columnList = "private_estate_batch_id",
    ),
  ],
)
open class CreditCredit {
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
  open var amount: Long = 0L

  @Size(max = 250)
  @Column(name = "prisoner_number", length = 250)
  open var prisonerNumber: String? = null

  @Column(name = "prisoner_dob")
  open var prisonerDob: LocalDate? = null

  @Column(name = "received_at")
  open var receivedAt: OffsetDateTime? = null

  @Size(max = 250)
  @Column(name = "prisoner_name", length = 250)
  open var prisonerName: String? = null

  @Size(max = 50)
  @NotNull
  @Column(name = "resolution", nullable = false, length = 50)
  open var resolution: String = ""

  @NotNull
  @Column(name = "reconciled", nullable = false)
  open var reconciled: Boolean = false

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  open var owner: AuthUser? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prison_id")
  open var prison: PrisonPrison? = null

  @NotNull
  @Column(name = "reviewed", nullable = false)
  open var reviewed: Boolean = false

  @NotNull
  @Column(name = "blocked", nullable = false)
  open var blocked: Boolean = false

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prisoner_profile_id")
  open var prisonerProfile: SecurityPrisonerprofile? =
    null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_profile_id")
  open var senderProfile: SecuritySenderprofile? = null

  @Size(max = 50)
  @Column(name = "nomis_transaction_id", length = 50)
  open var nomisTransactionId: String? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "private_estate_batch_id")
  open var privateEstateBatch: CreditPrivateestatebatch? =
    null

  @NotNull
  @Column(name = "is_counted_in_sender_profile_total", nullable = false)
  open var isCountedInSenderProfileTotal: Boolean = false

  @NotNull
  @Column(name = "is_counted_in_prisoner_profile_total", nullable = false)
  open var isCountedInPrisonerProfileTotal: Boolean = false

  // Manually-maintained back-references. Do not regenerate from DB — Django's
  // schema only models forward FKs; the existing services/resources rely on
  // these reverse navigations.
  @OneToOne(mappedBy = "credit", fetch = FetchType.LAZY)
  open var payment: PaymentPayment? = null

  @OneToOne(mappedBy = "credit", fetch = FetchType.LAZY)
  open var transaction: TransactionTransaction? = null

  @OneToOne(mappedBy = "credit", fetch = FetchType.LAZY)
  open var securityCheck: SecurityCheck? = null

  @OneToMany(mappedBy = "credit", fetch = FetchType.LAZY)
  open var logs: MutableList<CreditLog> = mutableListOf()

  @OneToMany(mappedBy = "credit", fetch = FetchType.LAZY)
  open var comments: MutableList<CreditComment> = mutableListOf()
}

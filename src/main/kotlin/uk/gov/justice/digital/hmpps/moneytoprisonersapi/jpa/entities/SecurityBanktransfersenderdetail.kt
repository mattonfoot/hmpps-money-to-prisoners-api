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
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_banktransfersenderdetails",
  schema = "public",
  indexes = [
    Index(
      name = "security_banktransfersenderdetails_sender_id_73db9d3e",
      columnList = "sender_id",
    ),
    Index(
      name = "security_banktransfersende_sender_bank_account_id_4075aa39",
      columnList = "sender_bank_account_id",
    ),
  ],
)
open class SecurityBanktransfersenderdetail {
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

  @Size(max = 250)
  @NotNull
  @Column(name = "sender_name", nullable = false, length = 250)
  open var senderName: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  open var sender: SecuritySenderprofile? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_bank_account_id", nullable = false)
  open var senderBankAccount: SecurityBankaccount? = null
}

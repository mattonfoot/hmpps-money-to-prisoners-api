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
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_banktransferrecipientdetails",
  schema = "public",
  indexes = [
    Index(
      name = "security_banktransferrecipientdetails_recipient_id_12c22f8b",
      columnList = "recipient_id",
    ),
    Index(
      name = "security_banktransferrecip_recipient_bank_account_id_206dcca7",
      columnList = "recipient_bank_account_id",
    ),
  ],
)
open class SecurityBanktransferrecipientdetail {
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
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipient_id", nullable = false)
  open var recipient: SecurityRecipientprofile? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipient_bank_account_id", nullable = false)
  open var recipientBankAccount: SecurityBankaccount? = null
}

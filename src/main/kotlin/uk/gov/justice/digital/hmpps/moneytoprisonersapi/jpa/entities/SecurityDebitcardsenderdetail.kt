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
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_debitcardsenderdetails",
  schema = "public",
  indexes = [
    Index(
      name = "security_debitcardsender_card_number_last_digits_2b6181a2_like",
      columnList = "card_number_last_digits",
    ),
    Index(
      name = "security_debitcardsenderde_card_number_last_digits_2b6181a2",
      columnList = "card_number_last_digits",
    ),
    Index(
      name = "security_debitcardsenderdetails_sender_id_e7c4567e",
      columnList = "sender_id",
    ),
    Index(
      name = "security_debitcardsenderdetails_postcode_99233649",
      columnList = "postcode",
    ),
    Index(
      name = "security_debitcardsenderdetails_postcode_99233649_like",
      columnList = "postcode",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_debitcardsender_card_number_last_digits__01ca4c78_uniq",
      columnNames = [
        "card_number_last_digits",
        "card_expiry_date",
        "postcode",
      ],
    ),
  ],
)
open class SecurityDebitcardsenderdetail {
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

  @Size(max = 4)
  @Column(name = "card_number_last_digits", length = 4)
  open var cardNumberLastDigits: String? = null

  @Size(max = 5)
  @Column(name = "card_expiry_date", length = 5)
  open var cardExpiryDate: String? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  open var sender: SecuritySenderprofile? = null

  @Size(max = 250)
  @Column(name = "postcode", length = 250)
  open var postcode: String? = null
}

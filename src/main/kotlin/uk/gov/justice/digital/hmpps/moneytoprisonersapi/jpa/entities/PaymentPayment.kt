package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.net.InetAddress
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
  name = "payment_payment",
  schema = "public",
  indexes = [
    Index(
      name = "payment_pay_modifie_c1f247_idx",
      columnList = "modified",
    ),
    Index(
      name = "payment_payment_status_2b87fefd",
      columnList = "status",
    ),
    Index(
      name = "payment_payment_status_2b87fefd_like",
      columnList = "status",
    ),
    Index(
      name = "payment_payment_processor_id_56f951bf",
      columnList = "processor_id",
    ),
    Index(
      name = "payment_payment_processor_id_56f951bf_like",
      columnList = "processor_id",
    ),
    Index(
      name = "payment_payment_batch_id_8ba8f52c",
      columnList = "batch_id",
    ),
    Index(
      name = "payment_payment_ip_address_91cbf960",
      columnList = "ip_address",
    ),
    Index(
      name = "payment_payment_billing_address_id_8f2c8dc5",
      columnList = "billing_address_id",
    ),
    Index(
      name = "payment_payment_worldpay_id_5b29896c",
      columnList = "worldpay_id",
    ),
    Index(
      name = "payment_payment_worldpay_id_5b29896c_like",
      columnList = "worldpay_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "payment_payment_credit_id_key",
      columnNames = ["credit_id"],
    ),
  ],
)
open class PaymentPayment {
  @Id
  @Column(name = "uuid", nullable = false)
  open var uuid: UUID? = null

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "modified", nullable = false)
  open var modified: OffsetDateTime = OffsetDateTime.now()

  @Size(max = 50)
  @NotNull
  @Column(name = "status", nullable = false, length = 50)
  open var status: String = ""

  @Size(max = 250)
  @Column(name = "processor_id", length = 250)
  open var processorId: String? = null

  @NotNull
  @Column(name = "amount", nullable = false)
  open var amount: Int = 0

  @Size(max = 250)
  @Column(name = "recipient_name", length = 250)
  open var recipientName: String? = null

  @NotNull
  @Column(name = "service_charge", nullable = false)
  open var serviceCharge: Int = 0

  @Size(max = 254)
  @Column(name = "email", length = 254)
  open var email: String? = null

  @NotNull
  @OneToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "credit_id", nullable = false)
  open var credit: CreditCredit? = null

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "batch_id")
  open var batch: PaymentBatch? = null

  @Size(max = 5)
  @Column(name = "card_expiry_date", length = 5)
  open var cardExpiryDate: String? = null

  @Size(max = 4)
  @Column(name = "card_number_last_digits", length = 4)
  open var cardNumberLastDigits: String? = null

  @Size(max = 250)
  @Column(name = "cardholder_name", length = 250)
  open var cardholderName: String? = null

  @Column(name = "ip_address")
  open var ipAddress: InetAddress? = null

  @Size(max = 250)
  @Column(name = "card_brand", length = 250)
  open var cardBrand: String? = null

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "billing_address_id")
  open var billingAddress: PaymentBillingaddress? = null

  @Size(max = 6)
  @Column(name = "card_number_first_digits", length = 6)
  open var cardNumberFirstDigits: String? = null

  @Size(max = 250)
  @Column(name = "worldpay_id", length = 250)
  open var worldpayId: String? = null
}

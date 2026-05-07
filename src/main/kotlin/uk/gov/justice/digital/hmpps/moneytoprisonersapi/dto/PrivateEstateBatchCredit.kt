package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * Credit details as included in a private estate batch's credits list.
 * Mirrors Python's `PrivateEstateBatchCredit` schema.
 */
@Schema(name = "PrivateEstateBatchCredit", description = "Credit details inside a private estate batch")
data class PrivateEstateBatchCredit(
  val id: Long?,
  @JsonProperty("prisoner_name")
  val prisonerName: String? = null,
  @JsonProperty("prisoner_number")
  val prisonerNumber: String? = null,
  @Schema(description = "Amount in pence", required = true)
  val amount: Long,
  @JsonProperty("started_at")
  val startedAt: OffsetDateTime? = null,
  @JsonProperty("received_at")
  val receivedAt: OffsetDateTime? = null,
  @JsonProperty("sender_name")
  val senderName: String? = null,
  @JsonProperty("sender_email")
  val senderEmail: String? = null,
  val prison: String? = null,
  val owner: String? = null,
  @JsonProperty("owner_name")
  val ownerName: String? = null,
  val resolution: String? = null,
  @JsonProperty("credited_at")
  val creditedAt: OffsetDateTime? = null,
  @JsonProperty("refunded_at")
  val refundedAt: OffsetDateTime? = null,
  @JsonProperty("set_manual_at")
  val setManualAt: OffsetDateTime? = null,
  val source: String? = null,
  @JsonProperty("intended_recipient")
  val intendedRecipient: String? = null,
  val anonymous: Boolean? = null,
  @JsonProperty("reconciliation_code")
  val reconciliationCode: String? = null,
  val comments: List<CommentDto> = emptyList(),
  val reviewed: Boolean? = null,
  @JsonProperty("short_payment_ref")
  val shortPaymentRef: String? = null,
  @JsonProperty("nomis_transaction_id")
  val nomisTransactionId: String? = null,
  @JsonProperty("billing_address")
  @Schema(description = "Billing address for this credit", required = true)
  val billingAddress: BillingAddress,
)

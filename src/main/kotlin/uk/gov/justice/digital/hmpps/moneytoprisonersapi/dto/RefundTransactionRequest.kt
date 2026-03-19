package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for bulk refunding transactions")
data class RefundTransactionRequest(
  @Schema(description = "List of transaction IDs to refund")
  @JsonProperty("transaction_ids")
  val transactionIds: List<Long> = emptyList(),
)

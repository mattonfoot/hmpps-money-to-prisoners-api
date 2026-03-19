package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for confirming disbursements")
data class DisbursementConfirmRequest(
  @Schema(description = "List of disbursements to confirm with optional NOMIS transaction IDs", required = true)
  val disbursements: List<DisbursementConfirmItem>,
)

@Schema(description = "Individual disbursement item in a confirm request")
data class DisbursementConfirmItem(
  @Schema(description = "Disbursement ID", example = "42", required = true)
  val id: Long,

  @Schema(description = "Optional NOMIS transaction ID", example = "TXN001")
  @JsonProperty("nomis_transaction_id")
  val nomisTransactionId: String? = null,
)

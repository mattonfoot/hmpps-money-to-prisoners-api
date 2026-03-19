package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request body for creating a comment on a disbursement")
data class DisbursementCommentRequest(
  @Schema(description = "Disbursement ID to attach comment to", example = "42", required = true)
  val disbursement: Long,

  @Schema(description = "Comment text (max 3000 characters)", example = "Approved by manager", required = true)
  @field:Size(max = 3000, message = "Comment must not exceed 3000 characters")
  val comment: String,

  @Schema(description = "Optional category (max 100 characters)", example = "GENERAL")
  @field:Size(max = 100, message = "Category must not exceed 100 characters")
  val category: String? = null,
)

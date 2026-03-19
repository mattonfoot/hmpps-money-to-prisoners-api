package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request body for creating a comment on a credit")
data class CreateCommentRequest(
  @Schema(description = "Credit ID to attach comment to", example = "42", required = true)
  val credit: Long,
  @Schema(description = "Comment text (max 3000 characters)", example = "Sender details verified", required = true)
  @field:Size(max = 3000, message = "Comment must not exceed 3000 characters")
  val comment: String,
)

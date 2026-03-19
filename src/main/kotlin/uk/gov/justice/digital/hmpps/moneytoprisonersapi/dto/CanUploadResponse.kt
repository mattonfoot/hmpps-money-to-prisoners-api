package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response indicating whether prisoner location upload is possible")
data class CanUploadResponse(
  @Schema(description = "Whether upload is currently allowed", example = "true")
  val canUpload: Boolean,
)

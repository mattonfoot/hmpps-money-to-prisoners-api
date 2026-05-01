package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(hidden = true)
data class CanUploadResponse(
  @Schema(description = "Whether upload is currently allowed", example = "true")
  val canUpload: Boolean,
)

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request body for reconciling taken payments into a batch")
data class ReconcilePaymentsRequest(
  @Schema(description = "Include credits received at or after this datetime", example = "2024-01-01T00:00:00")
  @JsonProperty("received_at__gte")
  val receivedAtGte: LocalDateTime,

  @Schema(description = "Include credits received before this datetime (exclusive)", example = "2024-02-01T00:00:00")
  @JsonProperty("received_at__lt")
  val receivedAtLt: LocalDateTime,
)

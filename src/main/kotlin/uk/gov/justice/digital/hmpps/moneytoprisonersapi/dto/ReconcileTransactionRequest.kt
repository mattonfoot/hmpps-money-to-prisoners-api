package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request body for reconciling transactions within a date range")
data class ReconcileTransactionRequest(
  @Schema(description = "Inclusive lower bound datetime for received_at filter", example = "2024-01-01T00:00:00")
  @JsonProperty("received_at__gte")
  val receivedAtGte: LocalDateTime? = null,

  @Schema(description = "Exclusive upper bound datetime for received_at filter", example = "2024-02-01T00:00:00")
  @JsonProperty("received_at__lt")
  val receivedAtLt: LocalDateTime? = null,
)

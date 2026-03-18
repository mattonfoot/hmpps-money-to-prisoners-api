package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "A single credit action item indicating whether a credit was posted to NOMIS")
data class CreditActionItem(
  @Schema(description = "The ID of the credit to action", required = true)
  @field:NotNull
  val id: Long?,

  @Schema(description = "Whether the credit was successfully posted to NOMIS", required = true)
  @field:NotNull
  val credited: Boolean?,

  @Schema(description = "The NOMIS transaction ID returned on successful posting", required = false)
  @JsonProperty("nomis_transaction_id")
  val nomisTransactionId: String? = null,
)

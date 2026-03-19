package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A group of credits that were credited on the same date by the same owner")
data class ProcessedCreditGroupDto(
  @Schema(description = "The date the credits were logged as credited (UTC date)", example = "2024-03-15")
  @JsonProperty("logged_at")
  val loggedAt: LocalDate,
  @Schema(description = "Username of the clerk who credited the prisoners", example = "clerk1")
  val owner: String,
  @Schema(description = "Full name of the owner (currently returns Unknown)", example = "Unknown")
  @JsonProperty("owner_name")
  val ownerName: String,
  @Schema(description = "Number of credits in this group", example = "5")
  val count: Int,
  @Schema(description = "Total amount in pence for all credits in this group", example = "50000")
  val total: Long,
  @Schema(description = "Number of comments across all credits in this group", example = "2")
  @JsonProperty("comment_count")
  val commentCount: Int,
)

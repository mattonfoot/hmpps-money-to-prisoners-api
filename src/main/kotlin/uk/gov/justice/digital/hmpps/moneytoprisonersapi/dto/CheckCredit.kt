package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * Detailed view of a security check including the embedded credit details.
 * Mirrors Python's `CheckCredit` schema (returned from /security/checks/{id}/).
 */
@Schema(name = "CheckCredit", description = "Detailed security check with embedded credit")
data class CheckCredit(
  val id: Long?,
  @Schema(description = "The credit being checked", required = true)
  val credit: SecurityCredit,
  val status: String?,
  val description: String?,
  val rules: List<String> = emptyList(),
  @JsonProperty("actioned_at")
  val actionedAt: OffsetDateTime? = null,
  @JsonProperty("actioned_by")
  val actionedBy: Long? = null,
  @JsonProperty("assigned_to")
  val assignedTo: Long? = null,
  @JsonProperty("decision_reason")
  val decisionReason: String? = null,
  @JsonProperty("actioned_by_name")
  val actionedByName: String? = null,
  @JsonProperty("assigned_to_name")
  val assignedToName: String? = null,
  @JsonProperty("rejection_reasons")
  val rejectionReasons: List<String> = emptyList(),
  @JsonProperty("auto_accept_rule_state")
  val autoAcceptRuleState: CheckAutoAcceptRuleState? = null,
)

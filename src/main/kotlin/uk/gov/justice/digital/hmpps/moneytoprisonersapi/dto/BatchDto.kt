package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Batch
import java.time.LocalDateTime

@Schema(description = "A processing batch containing a set of credits")
data class BatchDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,
  @Schema(description = "Username of the user who created the batch", example = "clerk1")
  val owner: String,
  @Schema(description = "List of credit IDs in this batch")
  @JsonProperty("credit_ids")
  val creditIds: List<Long>,
  @Schema(description = "Timestamp when the batch was created", example = "2024-03-15T10:30:00")
  val created: LocalDateTime?,
) {
  companion object {
    fun from(batch: Batch): BatchDto = BatchDto(
      id = batch.id,
      owner = batch.owner,
      creditIds = batch.credits.mapNotNull { it.id },
      created = batch.created,
    )
  }
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementComment
import java.time.LocalDateTime

@Schema(description = "A comment on a disbursement")
data class DisbursementCommentDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,

  @Schema(description = "Disbursement ID", example = "42")
  val disbursement: Long?,

  @Schema(description = "Comment text", example = "Approved by manager")
  val comment: String,

  @Schema(description = "Optional category", example = "GENERAL")
  val category: String?,

  @Schema(description = "User who created the comment", example = "clerk1")
  @JsonProperty("user_id")
  val userId: String?,

  @Schema(description = "Timestamp when record was created", example = "2024-03-15T10:30:00")
  val created: LocalDateTime?,

  @Schema(description = "Timestamp when record was last modified", example = "2024-03-15T10:30:00")
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(comment: DisbursementComment): DisbursementCommentDto = DisbursementCommentDto(
      id = comment.id,
      disbursement = comment.disbursement?.id,
      comment = comment.comment,
      category = comment.category,
      userId = comment.userId,
      created = comment.created,
      modified = comment.modified,
    )
  }
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Comment
import java.time.LocalDateTime

@Schema(description = "A comment on a credit record")
data class CommentDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,
  @Schema(description = "Credit ID this comment belongs to", example = "42")
  val credit: Long?,
  @Schema(description = "Comment text", example = "Sender details verified")
  val comment: String,
  @Schema(description = "User who created the comment", example = "user1")
  val userId: String?,
  @Schema(description = "Timestamp when the comment was created", example = "2024-03-15T10:30:00")
  val created: LocalDateTime?,
  @Schema(description = "Timestamp when the comment was last modified", example = "2024-03-15T10:30:00")
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(comment: Comment): CommentDto = CommentDto(
      id = comment.id,
      credit = comment.credit?.id,
      comment = comment.comment,
      userId = comment.userId,
      created = comment.created,
      modified = comment.modified,
    )
  }
}

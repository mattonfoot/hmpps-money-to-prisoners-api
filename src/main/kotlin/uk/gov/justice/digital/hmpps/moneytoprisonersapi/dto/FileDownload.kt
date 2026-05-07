package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime

@Schema(description = "File download record")
data class FileDownload(
  @Schema(description = "Record ID")
  val id: Long?,

  @Schema(description = "Label identifying the type of file download")
  val label: String,

  @Schema(description = "Date the file was downloaded")
  val date: LocalDate,

  @Schema(description = "Timestamp when the record was created")
  val created: OffsetDateTime?,

  @Schema(description = "Timestamp when the record was last modified")
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(entity: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FileDownload) = FileDownload(
      id = entity.id,
      label = entity.label,
      date = entity.date,
      created = entity.created,
      modified = entity.modified,
    )
  }
}

@Schema(hidden = true)
data class CreateFileDownloadRequest(
  @Schema(description = "Label identifying the type of file download")
  val label: String?,

  @Schema(description = "Date the file was downloaded (YYYY-MM-DD)")
  val date: String?,
)

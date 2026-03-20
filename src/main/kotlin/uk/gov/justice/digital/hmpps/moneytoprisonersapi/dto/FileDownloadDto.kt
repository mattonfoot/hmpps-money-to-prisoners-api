package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FileDownload
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "File download record")
data class FileDownloadDto(
  @Schema(description = "Record ID")
  val id: Long?,

  @Schema(description = "Label identifying the type of file download")
  val label: String,

  @Schema(description = "Date the file was downloaded")
  val date: LocalDate,

  @Schema(description = "Timestamp when the record was created")
  val created: LocalDateTime?,

  @Schema(description = "Timestamp when the record was last modified")
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(entity: FileDownload) = FileDownloadDto(
      id = entity.id,
      label = entity.label,
      date = entity.date,
      created = entity.created,
      modified = entity.modified,
    )
  }
}

@Schema(description = "Request body for creating a file download record")
data class CreateFileDownloadRequest(
  @Schema(description = "Label identifying the type of file download")
  val label: String?,

  @Schema(description = "Date the file was downloaded (YYYY-MM-DD)")
  val date: String?,
)

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.JobInformation
import java.time.LocalDateTime

@Schema(description = "Job information submitted alongside an account request")
data class JobInformationDto(
  @Schema(description = "Record ID")
  val id: Long?,

  @Schema(description = "ID of the user who submitted this job info (AUTH-071)")
  val user: Long?,

  @Schema(description = "Job title")
  val title: String,

  @Schema(description = "Prison estate")
  val prisonEstate: String,

  @Schema(description = "Tasks performed in the role")
  val tasks: String,

  @Schema(description = "Timestamp when the record was created")
  val created: LocalDateTime?,

  @Schema(description = "Timestamp when the record was last modified")
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(info: JobInformation) = JobInformationDto(
      id = info.id,
      user = info.user.id,
      title = info.title,
      prisonEstate = info.prisonEstate,
      tasks = info.tasks,
      created = info.created,
      modified = info.modified,
    )
  }
}

@Schema(description = "Request body for creating job information")
data class CreateJobInformationRequest(
  @Schema(description = "Job title")
  val title: String?,

  @Schema(description = "Prison estate")
  val prisonEstate: String?,

  @Schema(description = "Tasks performed in the role")
  val tasks: String?,
)

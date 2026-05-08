package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Job information submitted alongside an account request")
data class JobInformation(
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
  val created: OffsetDateTime?,

  @Schema(description = "Timestamp when the record was last modified")
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(info: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.JobInformation) = JobInformation(
      id = info.id,
      user = info.user?.id,
      title = info.title,
      prisonEstate = info.prisonEstate,
      tasks = info.tasks,
      created = info.created,
      modified = info.modified,
    )
  }
}

@Schema(hidden = true)
data class CreateJobInformationRequest(
  @Schema(description = "Job title")
  val title: String?,

  @Schema(description = "Prison estate")
  val prisonEstate: String?,

  @Schema(description = "Tasks performed in the role")
  val tasks: String?,
)

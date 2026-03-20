package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateJobInformationRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.JobInformationDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.JobInformationService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.UserService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping(produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Job Information", description = "Job information submitted alongside account requests (AUTH-070 to AUTH-072)")
class JobInformationResource(
  private val jobInformationService: JobInformationService,
  private val userService: UserService,
) {

  // -------------------------------------------------------------------------
  // AUTH-070, AUTH-071, AUTH-072: POST /job_information/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Submit job information",
    description = "Creates a job information record linked to the authenticated user (AUTH-071). " +
      "Authentication required (AUTH-072).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Job information created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = JobInformationDto::class))],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/job_information/")
  fun createJobInformation(
    @RequestBody request: CreateJobInformationRequest,
    authentication: Authentication,
  ): ResponseEntity<Any> {
    val user = userService.findByUsername(authentication.name)
      ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

    val info = jobInformationService.createJobInformation(
      user = user,
      title = request.title ?: "",
      prisonEstate = request.prisonEstate ?: "",
      tasks = request.tasks ?: "",
    )
    return ResponseEntity.status(HttpStatus.CREATED).body(JobInformationDto.from(info))
  }
}

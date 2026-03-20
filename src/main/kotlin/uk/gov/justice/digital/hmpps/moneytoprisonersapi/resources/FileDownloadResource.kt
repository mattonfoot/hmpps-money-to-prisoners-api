package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateFileDownloadRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.FileDownloadDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.FileDownloadService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.format.DateTimeParseException

@RestController
@RequestMapping(produces = ["application/json"])
@Tag(name = "File Downloads", description = "File download tracking endpoints (COR-001 to COR-003)")
class FileDownloadResource(
  private val fileDownloadService: FileDownloadService,
) {

  // -------------------------------------------------------------------------
  // COR-001: GET /file-downloads/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "List file download records",
    description = "Returns all file download records, ordered by date descending (COR-001).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of file download records",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = FileDownloadPaginatedResponse::class))],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/file-downloads/")
  fun listFileDownloads(): ResponseEntity<PaginatedResponse<FileDownloadDto>> {
    val downloads = fileDownloadService.listDownloads().map { FileDownloadDto.from(it) }
    return ResponseEntity.ok(PaginatedResponse(count = downloads.size, results = downloads))
  }

  // -------------------------------------------------------------------------
  // COR-002: POST /file-downloads/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Create a file download record",
    description = "Creates a new file download record. Label and date must be unique together (COR-002).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "File download record created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = FileDownloadDto::class))],
      ),
      ApiResponse(responseCode = "400", description = "Invalid or missing parameters", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/file-downloads/")
  fun createFileDownload(
    @RequestBody request: CreateFileDownloadRequest,
  ): ResponseEntity<Any> {
    if (request.label.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("label" to listOf("This field is required")))
    }
    if (request.date.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("date" to listOf("This field is required")))
    }
    val parsedDate = try {
      LocalDate.parse(request.date)
    } catch (e: DateTimeParseException) {
      return ResponseEntity.badRequest().body(mapOf("date" to listOf("Date has wrong format. Use YYYY-MM-DD.")))
    }
    val saved = fileDownloadService.createDownload(request.label, parsedDate)
    return ResponseEntity.status(HttpStatus.CREATED).body(FileDownloadDto.from(saved))
  }

  // -------------------------------------------------------------------------
  // COR-003: GET /file-downloads/missing/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Report missing file downloads",
    description = "Given a label and a list of expected dates, returns which dates have no download record. " +
      "Dates before the earliest recorded download for the label are excluded (COR-003).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of missing dates (YYYY-MM-DD)",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(type = "string", example = "2024-01-15")))],
      ),
      ApiResponse(responseCode = "400", description = "Missing or invalid parameters", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/file-downloads/missing/")
  fun getMissingFileDownloads(
    @Parameter(description = "Label identifying the type of file download")
    @RequestParam(required = false)
    label: String?,
    @Parameter(description = "List of expected dates in YYYY-MM-DD format")
    @RequestParam(name = "date", required = false, defaultValue = "")
    dates: List<String>,
  ): ResponseEntity<Any> {
    if (label.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("label" to listOf("This field is required")))
    }
    if (dates.isEmpty()) {
      return ResponseEntity.badRequest().body(mapOf("date" to listOf("At least one date is required")))
    }
    val missing = fileDownloadService.findMissingDownloads(label, dates)
    return ResponseEntity.ok(missing.map { it.toString() })
  }
}

@Schema(name = "PaginatedResponseFileDownloadDto", description = "Paginated response containing file download records")
private class FileDownloadPaginatedResponse(
  @Schema(description = "Total number of results")
  val count: Int,
  @Schema(description = "URL of the next page, or null", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null", nullable = true)
  val previous: String?,
  @Schema(description = "List of file download records")
  val results: List<FileDownloadDto>,
)

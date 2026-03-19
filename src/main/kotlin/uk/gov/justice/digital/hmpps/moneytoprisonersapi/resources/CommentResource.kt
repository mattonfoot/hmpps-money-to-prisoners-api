package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CommentDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateCommentRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Comment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CommentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal

@RestController
@RequestMapping("/comments", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Comments", description = "Endpoints for managing credit comments")
class CommentResource(
  private val commentRepository: CommentRepository,
  private val creditRepository: CreditRepository,
) {

  @Operation(
    summary = "Create comments on credits",
    description = "Creates one or more comments on credit records. " +
      "The user (userId) is automatically set to the authenticated user. " +
      "Comment text is limited to 3000 characters. " +
      "Accepts an array of comment objects. " +
      "Returns 201 Created with the created comment data.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Comments created successfully",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request — comment exceeds 3000 characters",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized — requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/")
  @ResponseStatus(HttpStatus.CREATED)
  fun createComments(
    @RequestBody @Valid requests: List<CreateCommentRequest>,
    principal: Principal,
  ): List<CommentDto> {
    val created = requests.map { request ->
      val credit = creditRepository.findById(request.credit).orElse(null)
      val comment = Comment(
        comment = request.comment,
        credit = credit,
        userId = principal.name,
      )
      commentRepository.save(comment)
    }
    return created.map { CommentDto.from(it) }
  }
}

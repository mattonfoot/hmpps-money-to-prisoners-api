package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateSavedSearchRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SavedSearchDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdateSavedSearchRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SavedSearch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SavedSearchRepository
import java.security.Principal

@RestController
@RequestMapping("/security/searches", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Saved Searches", description = "User-specific saved search management")
class SavedSearchResource(
  private val repository: SavedSearchRepository,
) {

  @Operation(summary = "List current user's saved searches (SEC-122)")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/")
  fun listSearches(principal: Principal): List<SavedSearchDto> = repository.findByUsername(principal.name)
    .map { SavedSearchDto.from(it) }

  @Operation(summary = "Create a saved search (SEC-120 to SEC-121)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/")
  fun createSearch(
    @RequestBody request: CreateSavedSearchRequest,
    principal: Principal,
  ): ResponseEntity<SavedSearchDto> {
    val search = SavedSearch(
      username = principal.name,
      description = request.description,
      endpoint = request.endpoint,
      filters = request.filters,
    )
    val saved = repository.save(search)
    return ResponseEntity.status(HttpStatus.CREATED).body(SavedSearchDto.from(saved))
  }

  @Operation(summary = "Update a saved search (SEC-123)")
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}/")
  fun updateSearch(
    @PathVariable id: Long,
    @RequestBody request: UpdateSavedSearchRequest,
    principal: Principal,
  ): ResponseEntity<SavedSearchDto> {
    val search = repository.findByIdAndUsername(id, principal.name)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SavedSearch $id not found")
    request.description?.let { search.description = it }
    request.endpoint?.let { search.endpoint = it }
    request.filters?.let { search.filters = it }
    val saved = repository.save(search)
    return ResponseEntity.ok(SavedSearchDto.from(saved))
  }

  @Operation(summary = "Delete a saved search (SEC-124)")
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}/")
  fun deleteSearch(
    @PathVariable id: Long,
    principal: Principal,
  ): ResponseEntity<Void> {
    val search = repository.findByIdAndUsername(id, principal.name)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SavedSearch $id not found")
    repository.delete(search)
    return ResponseEntity.noContent().build()
  }
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_SEARCHES
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateSavedSearchRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SavedSearch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdateSavedSearchRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SavedSearchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SearchFilterRepository
import java.security.Principal
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SavedSearch as SavedSearchEntity

@RestController
@RequestMapping("/searches", produces = ["application/json"])
@SecurityRequirement(name = "oauth2_provider")
@Tag(name = TAG_SEARCHES)
class SearchesResource(
  private val repository: SavedSearchRepository,
  private val searchFilterRepository: SearchFilterRepository,
  private val userRepository: AuthUserRepository,
) {

  @Operation(summary = "List current user's saved searches (SEC-122)")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/")
  fun listSearches(
    principal: Principal,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<SavedSearch> {
    val results = repository.findByUserUsername(principal.name).map { SavedSearch.from(it) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Create a saved search (SEC-120 to SEC-121)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/")
  fun createSearch(
    @RequestBody request: CreateSavedSearchRequest,
    principal: Principal,
  ): ResponseEntity<SavedSearch> {
    val owner = userRepository.findByUsername(principal.name)
    val search = SavedSearchEntity().apply {
      user = owner
      description = request.description
      endpoint = request.endpoint
      lastResultCount = request.lastResultCount ?: 0
      siteUrl = request.siteUrl
    }
    val saved = repository.save(search)
    // filters live on SecuritySearchfilter child rows. Persist them separately.
    persistFilters(saved, request.filters)
    return ResponseEntity.status(HttpStatus.CREATED).body(
      SavedSearch.from(saved, searchFilterRepository.findBySavedSearch(saved)),
    )
  }

  private fun persistFilters(search: SavedSearchEntity, filtersJson: String?) {
    if (filtersJson.isNullOrBlank()) return
    val parsed = parseFiltersJson(filtersJson)
    parsed.forEach { (field, value) ->
      val sf = uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecuritySearchfilter().apply {
        this.field = field
        this.value = value
        this.savedSearch = search
      }
      searchFilterRepository.save(sf)
    }
  }

  private fun parseFiltersJson(json: String): List<Pair<String, String>> = try {
    @Suppress("UNCHECKED_CAST")
    val raw = com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List::class.java)
    raw.mapNotNull {
      val m = it as? Map<*, *> ?: return@mapNotNull null
      val field = m["field"]?.toString() ?: return@mapNotNull null
      val value = m["value"]?.toString() ?: ""
      field to value
    }
  } catch (_: Exception) {
    emptyList()
  }

  @Operation(summary = "Update a saved search (SEC-123)")
  @PreAuthorize("isAuthenticated()")
  @RequestMapping(value = ["/{id}/"], method = [RequestMethod.PATCH, RequestMethod.PUT])
  fun updateSearch(
    @PathVariable id: Long,
    @RequestBody request: UpdateSavedSearchRequest,
    principal: Principal,
  ): ResponseEntity<SavedSearch> {
    val search = repository.findByIdAndUserUsername(id, principal.name)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SavedSearch $id not found")
    request.description?.let { search.description = it }
    request.endpoint?.let { search.endpoint = it }
    request.lastResultCount?.let { search.lastResultCount = it }
    request.siteUrl?.let { search.siteUrl = it }
    // filters: replace child rows wholesale to mirror Django's update behaviour.
    request.filters?.let { json ->
      searchFilterRepository.deleteAllBySavedSearch(search)
      persistFilters(search, json)
    }
    val saved = repository.save(search)
    return ResponseEntity.ok(SavedSearch.from(saved, searchFilterRepository.findBySavedSearch(saved)))
  }

  @Operation(summary = "Delete a saved search (SEC-124)")
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}/")
  fun deleteSearch(
    @PathVariable id: Long,
    principal: Principal,
  ): ResponseEntity<Void> {
    val search = repository.findByIdAndUserUsername(id, principal.name)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SavedSearch $id not found")
    repository.delete(search)
    return ResponseEntity.noContent().build()
  }
}

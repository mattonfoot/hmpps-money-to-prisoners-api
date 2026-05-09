package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

private val objectMapper = ObjectMapper()

@Schema(description = "A saved search belonging to a user")
data class SavedSearch(
  val id: Long?,
  val description: String,
  val endpoint: String,
  val filters: List<SearchFilter>,
  @JsonProperty("last_result_count")
  val lastResultCount: Int,
  @JsonProperty("site_url")
  val siteUrl: String?,
  val created: OffsetDateTime?,
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(
      search: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SavedSearch,
      filterRows: List<uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecuritySearchfilter> = emptyList(),
    ): SavedSearch = SavedSearch(
      id = search.id,
      description = search.description,
      endpoint = search.endpoint,
      filters = filterRows.map { SearchFilter(it.field, it.value) },
      lastResultCount = search.lastResultCount,
      siteUrl = search.siteUrl,
      created = search.created,
      modified = search.modified,
    )
  }
}

@Schema(hidden = true)
data class CreateSavedSearchRequest(
  val description: String,
  val endpoint: String,
  val filters: List<SearchFilter>? = null,
  @JsonProperty("last_result_count")
  val lastResultCount: Int? = null,
  @JsonProperty("site_url")
  val siteUrl: String? = null,
)

@Schema(hidden = true)
data class UpdateSavedSearchRequest(
  val description: String? = null,
  val endpoint: String? = null,
  val filters: List<SearchFilter>? = null,
  @JsonProperty("last_result_count")
  val lastResultCount: Int? = null,
  @JsonProperty("site_url")
  val siteUrl: String? = null,
)

@Schema(hidden = true)
data class MonitoredCountResponse(
  @JsonProperty("count")
  val count: Int,
)

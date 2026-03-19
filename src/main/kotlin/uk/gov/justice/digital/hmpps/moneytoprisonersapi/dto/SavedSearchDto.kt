package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SavedSearch
import java.time.LocalDateTime

@Schema(description = "A saved search belonging to a user")
data class SavedSearchDto(
  val id: Long?,
  val description: String,
  val endpoint: String,
  val filters: String?,
  val created: LocalDateTime?,
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(search: SavedSearch): SavedSearchDto = SavedSearchDto(
      id = search.id,
      description = search.description,
      endpoint = search.endpoint,
      filters = search.filters,
      created = search.created,
      modified = search.modified,
    )
  }
}

@Schema(description = "Request body for creating a saved search")
data class CreateSavedSearchRequest(
  val description: String,
  val endpoint: String,
  val filters: String? = null,
)

@Schema(description = "Request body for updating a saved search")
data class UpdateSavedSearchRequest(
  val description: String? = null,
  val endpoint: String? = null,
  val filters: String? = null,
)

@Schema(description = "Response containing the count of monitored senders + prisoners")
data class MonitoredCountResponse(
  @JsonProperty("count")
  val count: Int,
)

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonCategory

@Schema(description = "A prison category type")
data class Category(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,

  @Schema(description = "Category name", example = "Category A")
  val name: String,
) {
  companion object {
    fun from(category: PrisonCategory): Category = Category(
      id = category.id,
      name = category.name,
    )
  }
}

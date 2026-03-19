package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPopulation

@Schema(description = "A prison population type")
data class PrisonPopulationDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,

  @Schema(description = "Population name", example = "Adult")
  val name: String,
) {
  companion object {
    fun from(population: PrisonPopulation): PrisonPopulationDto = PrisonPopulationDto(
      id = population.id,
      name = population.name,
    )
  }
}

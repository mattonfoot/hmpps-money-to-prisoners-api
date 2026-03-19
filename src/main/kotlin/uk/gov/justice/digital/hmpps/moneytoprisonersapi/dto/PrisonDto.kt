package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison

@Schema(description = "A prison record")
data class PrisonDto(
  @Schema(description = "NOMIS prison ID", example = "LEI")
  val nomisId: String,

  @Schema(description = "Full prison name", example = "HMP Leeds")
  val name: String,

  @Schema(description = "Short name (without HMP/YOI/STC/IRC/HMYOI prefix)", example = "Leeds")
  val shortName: String,

  @Schema(description = "Region", example = "Yorkshire")
  val region: String,

  @Schema(description = "Whether pre-approval is required", example = "false")
  val preApprovalRequired: Boolean,

  @Schema(description = "Whether this is a private estate prison", example = "false")
  val privateEstate: Boolean,

  @Schema(description = "Whether NOMIS is used for balances", example = "true")
  val useNomisForBalances: Boolean,

  @Schema(description = "Categories assigned to this prison")
  val categories: List<String>,

  @Schema(description = "Populations assigned to this prison")
  val populations: List<String>,
) {
  companion object {
    private val PREFIXES = listOf("HMYOI ", "HMP ", "YOI ", "STC ", "IRC ")

    fun shortName(name: String): String {
      for (prefix in PREFIXES) {
        if (name.startsWith(prefix)) {
          return name.removePrefix(prefix)
        }
      }
      return name
    }

    fun from(prison: Prison): PrisonDto = PrisonDto(
      nomisId = prison.nomisId,
      name = prison.name,
      shortName = shortName(prison.name),
      region = prison.region,
      preApprovalRequired = prison.preApprovalRequired,
      privateEstate = prison.privateEstate,
      useNomisForBalances = prison.useNomisForBalances,
      categories = prison.categories.map { it.name },
      populations = prison.populations.map { it.name },
    )
  }
}

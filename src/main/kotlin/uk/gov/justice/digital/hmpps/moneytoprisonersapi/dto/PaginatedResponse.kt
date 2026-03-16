package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

data class PaginatedResponse<T>(
  val count: Int,
  val next: String? = null,
  val previous: String? = null,
  val results: List<T>,
)

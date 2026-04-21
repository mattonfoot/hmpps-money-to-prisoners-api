package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

data class PaginatedResponse<T>(
  val count: Int,
  val next: String? = null,
  val previous: String? = null,
  val results: List<T>,
) {
  companion object {
    /**
     * Creates a PaginatedResponse from a full list by applying offset/limit slicing.
     * Matches Django REST Framework's LimitOffsetPagination behaviour.
     */
    fun <T> fromList(items: List<T>, limit: Int = 20, offset: Int = 0): PaginatedResponse<T> {
      val total = items.size
      val start = offset.coerceAtMost(total)
      val end = (start + limit).coerceAtMost(total)
      val page = items.subList(start, end)
      return PaginatedResponse(
        count = total,
        results = page,
      )
    }
  }
}

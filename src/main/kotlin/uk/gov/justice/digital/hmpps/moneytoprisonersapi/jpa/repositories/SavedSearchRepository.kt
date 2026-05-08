package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SavedSearch

@Repository
interface SavedSearchRepository : JpaRepository<SavedSearch, Long> {

  fun findByUserUsername(username: String): List<SavedSearch>

  fun findByIdAndUserUsername(id: Long, username: String): SavedSearch?
}

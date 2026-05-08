package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecuritySavedsearch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecuritySearchfilter

@Repository
interface SearchFilterRepository : JpaRepository<SecuritySearchfilter, Long> {
  fun findBySavedSearch(savedSearch: SecuritySavedsearch): List<SecuritySearchfilter>

  @Modifying
  @Transactional
  fun deleteAllBySavedSearch(savedSearch: SecuritySavedsearch)
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MonitoredPartialEmailAddress

@Repository
interface MonitoredPartialEmailAddressRepository : JpaRepository<MonitoredPartialEmailAddress, Long> {

  fun findByKeywordIgnoreCase(keyword: String): MonitoredPartialEmailAddress?

  fun existsByKeywordIgnoreCase(keyword: String): Boolean

  fun findAllByOrderByKeywordAsc(): List<MonitoredPartialEmailAddress>
}

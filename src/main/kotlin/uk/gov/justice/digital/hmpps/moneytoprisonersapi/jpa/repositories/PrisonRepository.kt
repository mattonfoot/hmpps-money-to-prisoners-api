package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison

@Repository
interface PrisonRepository : JpaRepository<Prison, String> {
  fun findByRegionContainingIgnoreCase(region: String): List<Prison>

  @Query("SELECT p FROM Prison p JOIN p.categories c WHERE LOWER(c.name) = LOWER(:categoryName)")
  fun findByCategoryName(categoryName: String): List<Prison>

  @Query("SELECT p FROM Prison p JOIN p.populations pop WHERE LOWER(pop.name) = LOWER(:populationName)")
  fun findByPopulationName(populationName: String): List<Prison>
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerLocation
import java.time.LocalDateTime

@Repository
interface PrisonerLocationRepository : JpaRepository<PrisonerLocation, Long> {

  fun findByPrisonerNumberAndActiveTrue(prisonerNumber: String): List<PrisonerLocation>

  fun findByPrisonerNumberAndPrisonNomisIdAndActiveTrue(prisonerNumber: String, prisonNomisId: String): List<PrisonerLocation>

  fun findByPrisonerNumberAndPrisonerDobAndActiveTrue(prisonerNumber: String, prisonerDob: java.time.LocalDate): List<PrisonerLocation>

  fun existsByActiveFalseAndModifiedAfter(cutoff: LocalDateTime): Boolean

  @Modifying
  @Query("UPDATE PrisonerLocation pl SET pl.active = false, pl.modified = CURRENT_TIMESTAMP WHERE pl.active = true")
  fun deactivateAllActive(): Int

  fun deleteByActiveFalse()

  @Modifying
  @Query("UPDATE PrisonerLocation pl SET pl.active = false, pl.modified = CURRENT_TIMESTAMP WHERE pl.prisonerNumber = :prisonerNumber AND pl.prison.nomisId = :prisonNomisId AND pl.active = true")
  fun deactivateExistingForPrisonerAndPrison(prisonerNumber: String, prisonNomisId: String): Int

  @Modifying
  @Query("UPDATE PrisonerLocation pl SET pl.modified = CURRENT_TIMESTAMP WHERE pl.id = :id")
  fun updateModifiedToNow(id: Long)
}

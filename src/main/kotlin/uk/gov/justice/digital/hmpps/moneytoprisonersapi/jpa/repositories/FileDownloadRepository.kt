package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FileDownload
import java.time.LocalDate

interface FileDownloadRepository : JpaRepository<FileDownload, Long> {

  fun findAllByOrderByDateDesc(): List<FileDownload>

  @Query("SELECT MIN(f.date) FROM FileDownload f WHERE f.label = :label")
  fun findEarliestDateByLabel(@Param("label") label: String): LocalDate?

  @Query("SELECT f.date FROM FileDownload f WHERE f.label = :label AND f.date IN :dates")
  fun findDatesByLabelAndDateIn(
    @Param("label") label: String,
    @Param("dates") dates: List<LocalDate>,
  ): List<LocalDate>
}

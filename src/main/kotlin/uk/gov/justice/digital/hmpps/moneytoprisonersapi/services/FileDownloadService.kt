package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.CustomException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FileDownload
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.FileDownloadRepository
import java.time.LocalDate

class DuplicateFileDownloadException(label: String, date: LocalDate) : CustomException("File download already exists for label=$label, date=$date", HttpStatus.BAD_REQUEST)

@Service
class FileDownloadService(
  private val fileDownloadRepository: FileDownloadRepository,
) {

  /**
   * COR-002: Creates a new file download record (label + date must be unique).
   * Mirrors Python's `unique_together(label, date)` validation by rejecting
   * duplicates with a 400 rather than letting the DB constraint surface as 500.
   */
  @Transactional
  fun createDownload(label: String, date: LocalDate): FileDownload {
    if (fileDownloadRepository.existsByLabelAndDate(label, date)) {
      throw DuplicateFileDownloadException(label, date)
    }
    return fileDownloadRepository.save(
      FileDownload().apply {
        this.label = label
        this.date = date
      },
    )
  }

  /**
   * COR-003: Finds which requested dates are missing from the database for a given label.
   * Dates before the earliest recorded download are excluded to avoid false positives.
   */
  @Transactional(readOnly = true)
  fun findMissingDownloads(label: String, dates: List<String>): List<LocalDate> {
    val parsedDates = dates.map { LocalDate.parse(it) }
    val earliestDate = fileDownloadRepository.findEarliestDateByLabel(label) ?: return emptyList()
    val presentDates = fileDownloadRepository.findDatesByLabelAndDateIn(label, parsedDates).toSet()
    return parsedDates.filter { it >= earliestDate && it !in presentDates }
  }
}

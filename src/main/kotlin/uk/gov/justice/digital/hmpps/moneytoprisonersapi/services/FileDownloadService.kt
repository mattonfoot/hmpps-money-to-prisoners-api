package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FileDownload
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.FileDownloadRepository
import java.time.LocalDate

@Service
class FileDownloadService(
  private val fileDownloadRepository: FileDownloadRepository,
) {

  /**
   * COR-001: Lists all file downloads, newest first.
   */
  @Transactional(readOnly = true)
  fun listDownloads(): List<FileDownload> = fileDownloadRepository.findAllByOrderByDateDesc()

  /**
   * COR-002: Creates a new file download record (label + date must be unique).
   */
  @Transactional
  fun createDownload(label: String, date: LocalDate): FileDownload = fileDownloadRepository.save(FileDownload(label = label, date = date))

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

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FileDownload
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.FileDownloadRepository
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("FileDownloadService")
class FileDownloadServiceTest {

  @Mock
  private lateinit var fileDownloadRepository: FileDownloadRepository

  @InjectMocks
  private lateinit var service: FileDownloadService

  private fun makeDownload(
    id: Long = 1L,
    label: String = "bank_statement",
    date: LocalDate = LocalDate.of(2024, 1, 15),
  ) = FileDownload(
    id = id,
    label = label,
    date = date,
    created = LocalDateTime.now(),
    modified = LocalDateTime.now(),
  )

  @Nested
  @DisplayName("listDownloads")
  inner class ListDownloads {

    @Test
    fun `returns all downloads ordered by date descending`() {
      val downloads = listOf(makeDownload(id = 2L, date = LocalDate.of(2024, 1, 16)), makeDownload())
      whenever(fileDownloadRepository.findAllByOrderByDateDesc()).thenReturn(downloads)

      val result = service.listDownloads()

      assertThat(result).hasSize(2)
      assertThat(result[0].date).isAfter(result[1].date)
    }
  }

  @Nested
  @DisplayName("createDownload")
  inner class CreateDownload {

    @Test
    fun `COR-002 saves new download record`() {
      val date = LocalDate.of(2024, 1, 15)
      val saved = makeDownload(date = date)
      whenever(fileDownloadRepository.save(any())).thenReturn(saved)

      val result = service.createDownload("bank_statement", date)

      val captor = argumentCaptor<FileDownload>()
      verify(fileDownloadRepository).save(captor.capture())
      assertThat(captor.firstValue.label).isEqualTo("bank_statement")
      assertThat(captor.firstValue.date).isEqualTo(date)
      assertThat(result).isEqualTo(saved)
    }
  }

  @Nested
  @DisplayName("findMissingDownloads")
  inner class FindMissingDownloads {

    @Test
    fun `COR-003 returns dates not present in the database after earliest record`() {
      val label = "bank_statement"
      val requestedDates = listOf("2024-01-13", "2024-01-14", "2024-01-15")
      val earliestDate = LocalDate.of(2024, 1, 13)

      whenever(fileDownloadRepository.findEarliestDateByLabel(label)).thenReturn(earliestDate)
      whenever(fileDownloadRepository.findDatesByLabelAndDateIn(label, listOf(LocalDate.of(2024, 1, 13), LocalDate.of(2024, 1, 14), LocalDate.of(2024, 1, 15))))
        .thenReturn(listOf(LocalDate.of(2024, 1, 15)))

      val missing = service.findMissingDownloads(label, requestedDates)

      assertThat(missing).containsExactlyInAnyOrder(
        LocalDate.of(2024, 1, 13),
        LocalDate.of(2024, 1, 14),
      )
    }

    @Test
    fun `COR-003 excludes dates before earliest record to avoid false positives`() {
      val label = "bank_statement"
      // Earliest record is on 14th; 13th is before earliest so not reported as missing
      val requestedDates = listOf("2024-01-13", "2024-01-14", "2024-01-15")
      val earliestDate = LocalDate.of(2024, 1, 14)

      whenever(fileDownloadRepository.findEarliestDateByLabel(label)).thenReturn(earliestDate)
      whenever(fileDownloadRepository.findDatesByLabelAndDateIn(label, listOf(LocalDate.of(2024, 1, 13), LocalDate.of(2024, 1, 14), LocalDate.of(2024, 1, 15))))
        .thenReturn(listOf(LocalDate.of(2024, 1, 14)))

      val missing = service.findMissingDownloads(label, requestedDates)

      // 13th is before earliest record, so it's excluded from missing
      assertThat(missing).containsExactly(LocalDate.of(2024, 1, 15))
    }

    @Test
    fun `returns all requested dates as missing when no records exist for label`() {
      val label = "bank_statement"
      val requestedDates = listOf("2024-01-13", "2024-01-14")

      whenever(fileDownloadRepository.findEarliestDateByLabel(label)).thenReturn(null)

      val missing = service.findMissingDownloads(label, requestedDates)

      assertThat(missing).isEmpty()
    }
  }
}

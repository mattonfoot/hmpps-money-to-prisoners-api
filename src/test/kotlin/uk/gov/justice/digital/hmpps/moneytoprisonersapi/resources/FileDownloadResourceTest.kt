package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateFileDownloadRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.FileDownloadDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FileDownload
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.FileDownloadService
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("FileDownloadResource")
class FileDownloadResourceTest {

  @Mock
  private lateinit var fileDownloadService: FileDownloadService

  @InjectMocks
  private lateinit var resource: FileDownloadResource

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
  @DisplayName("GET /file-downloads/ (COR-001)")
  inner class ListFileDownloads {

    @Test
    fun `COR-001 returns paginated list of file downloads`() {
      val downloads = listOf(makeDownload(id = 1L), makeDownload(id = 2L))
      whenever(fileDownloadService.listDownloads()).thenReturn(downloads)

      val response = resource.listFileDownloads()

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body?.count).isEqualTo(2)
      assertThat(response.body?.results).hasSize(2)
    }

    @Test
    fun `returns empty list when no downloads exist`() {
      whenever(fileDownloadService.listDownloads()).thenReturn(emptyList())

      val response = resource.listFileDownloads()

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body?.count).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("POST /file-downloads/ (COR-002)")
  inner class CreateFileDownload {

    @Test
    fun `COR-002 returns 201 with created download record`() {
      val download = makeDownload()
      whenever(fileDownloadService.createDownload(any(), any())).thenReturn(download)

      val request = CreateFileDownloadRequest(label = "bank_statement", date = "2024-01-15")
      val response = resource.createFileDownload(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      val dto = response.body as? FileDownloadDto
      assertThat(dto?.label).isEqualTo("bank_statement")
    }

    @Test
    fun `returns 400 when label is missing`() {
      val request = CreateFileDownloadRequest(label = null, date = "2024-01-15")
      val response = resource.createFileDownload(request)
      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when date is missing`() {
      val request = CreateFileDownloadRequest(label = "bank_statement", date = null)
      val response = resource.createFileDownload(request)
      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when date format is invalid`() {
      val request = CreateFileDownloadRequest(label = "bank_statement", date = "not-a-date")
      val response = resource.createFileDownload(request)
      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
  }

  @Nested
  @DisplayName("GET /file-downloads/missing/ (COR-003)")
  inner class MissingFileDownloads {

    @Test
    fun `COR-003 returns missing dates for a label`() {
      val missingDates = listOf(LocalDate.of(2024, 1, 13), LocalDate.of(2024, 1, 14))
      whenever(
        fileDownloadService.findMissingDownloads(
          "bank_statement",
          listOf("2024-01-13", "2024-01-14"),
        ),
      ).thenReturn(missingDates)

      val response = resource.getMissingFileDownloads("bank_statement", listOf("2024-01-13", "2024-01-14"))

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      @Suppress("UNCHECKED_CAST")
      assertThat(response.body as List<String>).hasSize(2)
    }

    @Test
    fun `returns 400 when label is missing`() {
      val response = resource.getMissingFileDownloads(null, listOf("2024-01-13"))
      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when no dates provided`() {
      val response = resource.getMissingFileDownloads("bank_statement", emptyList())
      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
  }
}

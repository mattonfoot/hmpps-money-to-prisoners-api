package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("FileDownload entity")
class FileDownloadEntityTest {

  @Test
  fun `COR-001 has label field`() {
    val entity = FileDownload(label = "bank_statement", date = LocalDate.of(2024, 1, 15))
    assertThat(entity.label).isEqualTo("bank_statement")
  }

  @Test
  fun `COR-002 has date field`() {
    val date = LocalDate.of(2024, 1, 15)
    val entity = FileDownload(label = "bank_statement", date = date)
    assertThat(entity.date).isEqualTo(date)
  }

  @Test
  fun `timestamps are null before persisting`() {
    val entity = FileDownload(label = "bank_statement", date = LocalDate.of(2024, 1, 15))
    assertThat(entity.created).isNull()
    assertThat(entity.modified).isNull()
  }

  @Test
  fun `PrePersist sets both timestamps`() {
    val entity = FileDownload(label = "bank_statement", date = LocalDate.of(2024, 1, 15))
    entity.onCreate()
    assertThat(entity.created).isNotNull()
    assertThat(entity.modified).isNotNull()
    assertThat(entity.created).isEqualTo(entity.modified)
  }

  @Test
  fun `PreUpdate updates modified timestamp`() {
    val entity = FileDownload(label = "bank_statement", date = LocalDate.of(2024, 1, 15))
    entity.onCreate()
    val originalCreated = entity.created
    entity.onUpdate()
    assertThat(entity.created).isEqualTo(originalCreated)
    assertThat(entity.modified).isNotNull()
  }
}

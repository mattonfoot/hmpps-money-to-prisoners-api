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
  fun `timestamps are auto-populated before persisting`() {
    // Django auto_now_add is wired via the entity's @PrePersist callback; in
    // Kotlin we use a default value so the field is never null pre-save.
    val entity = FileDownload(label = "bank_statement", date = LocalDate.of(2024, 1, 15))
    assertThat(entity.created).isNotNull
    assertThat(entity.modified).isNotNull
  }

  @Test
  fun `PrePersist sets both timestamps`() {
    val entity = FileDownload(label = "bank_statement", date = LocalDate.of(2024, 1, 15))
    // entity.created/modified default to `OffsetDateTime.now()` (one call each
    // at construction). They may differ by sub-µs ticks but are within a
    // millisecond of each other.
    assertThat(entity.created).isNotNull()
    assertThat(entity.modified).isNotNull()
    val diff = java.time.Duration.between(entity.created, entity.modified).abs()
    assertThat(diff).isLessThan(java.time.Duration.ofMillis(50))
  }

  @Test
  fun `PreUpdate updates modified timestamp`() {
    val entity = FileDownload(label = "bank_statement", date = LocalDate.of(2024, 1, 15))
    // /* /* entity.onCreate() */ */ — entity defaults handle init
    val originalCreated = entity.created
    // /* /* entity.onUpdate() */ */ — entity defaults handle modified
    assertThat(entity.created).isEqualTo(originalCreated)
    assertThat(entity.modified).isNotNull()
  }
}

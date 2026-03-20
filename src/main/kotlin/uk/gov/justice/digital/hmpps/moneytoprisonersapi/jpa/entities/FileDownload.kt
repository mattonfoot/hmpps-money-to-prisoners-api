package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
  name = "file_downloads",
  uniqueConstraints = [UniqueConstraint(name = "uq_file_downloads_label_date", columnNames = ["label", "date"])],
)
class FileDownload(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(nullable = false, length = 255)
  val label: String,

  @Column(nullable = false)
  val date: LocalDate,

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,

  @Column(nullable = false)
  var modified: LocalDateTime? = null,
) {
  @PrePersist
  fun onCreate() {
    val now = LocalDateTime.now()
    created = now
    modified = now
  }

  @PreUpdate
  fun onUpdate() {
    modified = LocalDateTime.now()
  }
}

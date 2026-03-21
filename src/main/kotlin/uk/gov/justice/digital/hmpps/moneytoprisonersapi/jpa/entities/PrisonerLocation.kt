package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
  name = "prison_prisonerlocation",
  indexes = [
    Index(name = "idx_prisoner_locations_prisoner_number", columnList = "prisoner_number"),
    Index(name = "idx_prisoner_locations_active", columnList = "active"),
  ],
)
class PrisonerLocation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(name = "prisoner_number", nullable = false, length = 250)
  val prisonerNumber: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prison_id", nullable = false)
  val prison: Prison,

  @Column(nullable = false)
  var active: Boolean = true,

  @Column(name = "created_by", nullable = false, length = 250)
  val createdBy: String,

  @Column(name = "prisoner_dob")
  val prisonerDob: LocalDate? = null,

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

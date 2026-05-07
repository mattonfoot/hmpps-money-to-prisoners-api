package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
  name = "core_filedownload",
  schema = "public",
  indexes = [
    Index(
      name = "core_filedownload_label_bd2c9f85",
      columnList = "label",
    ),
    Index(
      name = "core_filedownload_label_bd2c9f85_like",
      columnList = "label",
    ),
    Index(
      name = "core_filedownload_date_cdeb387e",
      columnList = "date",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "core_filedownload_label_date_43104caf_uniq",
      columnNames = [
        "label",
        "date",
      ],
    ),
  ],
)
open class CoreFiledownload {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "modified", nullable = false)
  open var modified: OffsetDateTime = OffsetDateTime.now()

  @Size(max = 255)
  @NotNull
  @Column(name = "label", nullable = false)
  open var label: String = ""

  @NotNull
  @Column(name = "date", nullable = false)
  open var date: LocalDate = LocalDate.now()
}

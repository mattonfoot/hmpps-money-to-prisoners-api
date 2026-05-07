package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(name = "django_migrations", schema = "public")
open class DjangoMigration {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 255)
  @NotNull
  @Column(name = "app", nullable = false)
  open var app: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  open var name: String = ""

  @NotNull
  @Column(name = "applied", nullable = false)
  open var applied: OffsetDateTime = OffsetDateTime.now()
}

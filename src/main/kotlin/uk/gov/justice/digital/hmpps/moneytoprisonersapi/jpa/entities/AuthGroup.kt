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

@Entity
@Table(
  name = "auth_group",
  schema = "public",
  indexes = [
    Index(
      name = "auth_group_name_a6ea08ec_like",
      columnList = "name",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "auth_group_name_key",
      columnNames = ["name"],
    ),
  ],
)
open class AuthGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 150)
  @NotNull
  @Column(name = "name", nullable = false, length = 150)
  open var name: String = ""
}

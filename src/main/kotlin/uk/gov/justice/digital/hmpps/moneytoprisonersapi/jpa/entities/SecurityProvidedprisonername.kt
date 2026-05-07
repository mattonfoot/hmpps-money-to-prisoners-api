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
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(
  name = "security_providedprisonername",
  schema = "public",
  indexes = [
    Index(
      name = "security_prisonerrecipientname_prisoner_id_808eb7e3",
      columnList = "prisoner_id",
    ),
  ],
)
open class SecurityProvidedprisonername {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 250)
  @NotNull
  @Column(name = "name", nullable = false, length = 250)
  open var name: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prisoner_id", nullable = false)
  open var prisoner: SecurityPrisonerprofile? = null
}

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
  name = "prison_remittanceemail",
  schema = "public",
  indexes = [
    Index(
      name = "prison_remittanceemail_prison_id_04dde4cb",
      columnList = "prison_id",
    ),
    Index(
      name = "prison_remittanceemail_prison_id_04dde4cb_like",
      columnList = "prison_id",
    ),
  ],
)
open class PrisonRemittanceemail {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 254)
  @NotNull
  @Column(name = "email", nullable = false, length = 254)
  open var email: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null
}

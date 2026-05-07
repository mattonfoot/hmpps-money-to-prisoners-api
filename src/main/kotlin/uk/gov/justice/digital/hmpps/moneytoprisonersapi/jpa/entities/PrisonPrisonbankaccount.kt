package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(
  name = "prison_prisonbankaccount",
  schema = "public",
  indexes = [
    Index(
      name = "prison_prisonbankaccount_prison_id_20ddf959_like",
      columnList = "prison_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "prison_prisonbankaccount_prison_id_key",
      columnNames = ["prison_id"],
    ),
  ],
)
open class PrisonPrisonbankaccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 250)
  @NotNull
  @Column(name = "address_line1", nullable = false, length = 250)
  open var addressLine1: String = ""

  @Size(max = 250)
  @NotNull
  @Column(name = "address_line2", nullable = false, length = 250)
  open var addressLine2: String = ""

  @Size(max = 250)
  @NotNull
  @Column(name = "city", nullable = false, length = 250)
  open var city: String = ""

  @Size(max = 250)
  @NotNull
  @Column(name = "postcode", nullable = false, length = 250)
  open var postcode: String = ""

  @Size(max = 50)
  @NotNull
  @Column(name = "sort_code", nullable = false, length = 50)
  open var sortCode: String = ""

  @Size(max = 50)
  @NotNull
  @Column(name = "account_number", nullable = false, length = 50)
  open var accountNumber: String = ""

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null
}

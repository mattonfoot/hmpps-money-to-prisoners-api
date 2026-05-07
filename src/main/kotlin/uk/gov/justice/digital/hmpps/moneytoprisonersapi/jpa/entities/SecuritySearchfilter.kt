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
  name = "security_searchfilter",
  schema = "public",
  indexes = [
    Index(
      name = "security_searchfilter_saved_search_id_13775ad1",
      columnList = "saved_search_id",
    ),
  ],
)
open class SecuritySearchfilter {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 255)
  @NotNull
  @Column(name = "field", nullable = false)
  open var field: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "value", nullable = false)
  open var value: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "saved_search_id", nullable = false)
  open var savedSearch: SecuritySavedsearch? = null
}

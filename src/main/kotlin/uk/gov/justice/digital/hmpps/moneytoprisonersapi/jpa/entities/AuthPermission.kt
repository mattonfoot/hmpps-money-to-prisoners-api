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
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(
  name = "auth_permission",
  schema = "public",
  indexes = [
    Index(
      name = "auth_permission_content_type_id_2f476e4b",
      columnList = "content_type_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "auth_permission_content_type_id_codename_01ab375a_uniq",
      columnNames = [
        "content_type_id",
        "codename",
      ],
    ),
  ],
)
open class AuthPermission {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  open var name: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_type_id", nullable = false)
  open var contentType: DjangoContentType? = null

  @Size(max = 100)
  @NotNull
  @Column(name = "codename", nullable = false, length = 100)
  open var codename: String = ""
}

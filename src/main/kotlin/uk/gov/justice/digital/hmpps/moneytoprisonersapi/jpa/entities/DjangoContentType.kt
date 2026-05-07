package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(
  name = "django_content_type",
  schema = "public",
  uniqueConstraints = [
    UniqueConstraint(
      name = "django_content_type_app_label_model_76bd3d3b_uniq",
      columnNames = [
        "app_label",
        "model",
      ],
    ),
  ],
)
open class DjangoContentType {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 100)
  @NotNull
  @Column(name = "app_label", nullable = false, length = 100)
  open var appLabel: String = ""

  @Size(max = 100)
  @NotNull
  @Column(name = "model", nullable = false, length = 100)
  open var model: String = ""
}

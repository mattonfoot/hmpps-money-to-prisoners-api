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
import java.time.OffsetDateTime

@Entity
@Table(
  name = "disbursement_comment",
  schema = "public",
  indexes = [
    Index(
      name = "disbursement_comment_disbursement_id_25a38013",
      columnList = "disbursement_id",
    ),
    Index(
      name = "disbursement_comment_user_id_398a7e2b",
      columnList = "user_id",
    ),
  ],
)
open class DisbursementComment {
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

  @NotNull
  @Column(name = "comment", nullable = false, length = Integer.MAX_VALUE)
  open var comment: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "disbursement_id", nullable = false)
  open var disbursement: DisbursementDisbursement? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  open var user: AuthUser? = null

  @Size(max = 100)
  @NotNull
  @Column(name = "category", nullable = false, length = 100)
  open var category: String = ""
}

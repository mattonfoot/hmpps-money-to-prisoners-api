package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "credit_creditingtime", schema = "public")
open class CreditCreditingtime {
  @Id
  @Column(name = "credit_id", nullable = false)
  open var id: Long? = null

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "credit_id", nullable = false)
  open var creditCredit: CreditCredit? = null

  @Column(name = "crediting_time", columnDefinition = "interval")
  open var creditingTime: Any? = null
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities
enum class AccountRequestStatus(val value: String) {
  PENDING("pending"),
  ACCEPTED("accepted"),
  REJECTED("rejected"),
}

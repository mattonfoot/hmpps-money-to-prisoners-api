package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UserDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AccountRequestRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpRoleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository

@Service
class AccountRequestService(
  private val accountRequestRepository: AccountRequestRepository,
  private val mtpUserRepository: MtpUserRepository,
  private val mtpRoleRepository: MtpRoleRepository,
  private val prisonRepository: PrisonRepository,
) {

  /**
   * AUTH-061: Lists all pending account requests.
   * AUTH-067: Ordered by created ASC by default, or DESC when ordering="-created".
   */
  @Transactional(readOnly = true)
  fun listPendingRequests(ordering: String?): List<AccountRequest> = if (ordering == "-created") {
    accountRequestRepository.findAllPendingOrderByCreatedDesc()
  } else {
    accountRequestRepository.findAllPendingOrderByCreatedAsc()
  }

  /**
   * AUTH-060: Creates a new account request.
   * AUTH-062: Returns the existing user (if any) alongside the created request.
   */
  @Transactional
  fun createRequest(
    username: String,
    firstName: String,
    lastName: String,
    email: String,
    roleName: String?,
    prisonId: String?,
  ): Pair<AccountRequest, UserDto?> {
    val role = roleName?.let { mtpRoleRepository.findByName(it) }
    val prison = prisonId?.let { prisonRepository.findById(it).orElse(null) }
    val existingMtpUser = mtpUserRepository.findByUsernameIgnoreCase(username)

    val request = accountRequestRepository.save(
      AccountRequest().apply {
        this.username = username.lowercase()
        this.firstName = firstName
        this.lastName = lastName
        this.email = email
        this.role = role
        this.prison = prison
      },
    )

    val existingUser = existingMtpUser?.let { UserDto.from(it, false) }
    return request to existingUser
  }

  /**
   * AUTH-063: Accepts a pending request, creating or updating the MTP user.
   * Returns null if the request is not found.
   */
  fun getRequest(id: Long): AccountRequest? = accountRequestRepository.findById(id).orElse(null)

  @Transactional
  fun acceptRequest(id: Long): AccountRequest? {
    // Mirrors mtp_api/apps/mtp_auth/views.py AccountRequestViewSet.partial_update:
    // accepting creates/updates the AuthUser then DELETES the account_request row
    // (Django has no status column — presence-of-row IS the pending state).
    val request = accountRequestRepository.findById(id).orElse(null) ?: return null

    val existing = mtpUserRepository.findByUsernameIgnoreCase(request.username)
    if (existing != null) {
      // role change isn't directly settable on AuthUser — Role->Group association
      // applies via mtp_auth_role.other_groups. Stub the role-update path until we
      // wire in the role-to-group mapping helpers.
      // request.role?.let { /* TODO: update group memberships */ }
      mtpUserRepository.save(existing)
    } else {
      val newUser = MtpUser().apply {
        this.username = request.username
        this.firstName = request.firstName
        this.lastName = request.lastName
        this.email = request.email
        this.isActive = true
      }
      mtpUserRepository.save(newUser)
      // request.prison/role assignment via PrisonUserMapping + group joins is a
      // multi-step Django flow; stubbed for now.
    }

    accountRequestRepository.delete(request)
    return request
  }

  /**
   * AUTH-066: Rejects a pending request — Django deletes the row.
   */
  @Transactional
  fun rejectRequest(id: Long): AccountRequest? {
    val request = accountRequestRepository.findById(id).orElse(null) ?: return null
    accountRequestRepository.delete(request)
    return request
  }
}

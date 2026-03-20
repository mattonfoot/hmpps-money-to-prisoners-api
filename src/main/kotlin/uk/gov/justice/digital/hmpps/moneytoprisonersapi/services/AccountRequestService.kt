package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UserDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequestStatus
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
      AccountRequest(
        username = username.lowercase(),
        firstName = firstName,
        lastName = lastName,
        email = email,
        role = role,
        prison = prison,
      ),
    )

    val existingUser = existingMtpUser?.let { UserDto.from(it, false) }
    return request to existingUser
  }

  /**
   * AUTH-063: Accepts a pending request, creating or updating the MTP user.
   * Returns null if the request is not found.
   */
  @Transactional
  fun acceptRequest(id: Long): AccountRequest? {
    val request = accountRequestRepository.findById(id).orElse(null) ?: return null

    val existing = mtpUserRepository.findByUsernameIgnoreCase(request.username)
    if (existing != null) {
      // AUTH-063: role change — update the existing user's role
      request.role?.let { existing.role = it }
      mtpUserRepository.save(existing)
    } else {
      // AUTH-063: new account — create user from request
      val newUser = MtpUser(
        username = request.username,
        firstName = request.firstName,
        lastName = request.lastName,
        email = request.email,
        role = request.role,
      )
      request.prison?.let { newUser.prisons = mutableSetOf(it) }
      mtpUserRepository.save(newUser)
    }

    request.status = AccountRequestStatus.ACCEPTED.value
    return accountRequestRepository.save(request)
  }

  /**
   * AUTH-066: Rejects a pending request by setting its status to rejected.
   * Returns null if the request is not found.
   */
  @Transactional
  fun rejectRequest(id: Long): AccountRequest? {
    val request = accountRequestRepository.findById(id).orElse(null) ?: return null
    request.status = AccountRequestStatus.REJECTED.value
    return accountRequestRepository.save(request)
  }
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UserDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpAuthPrisonusermapping
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpRole
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AccountRequestRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpRoleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonUserMappingRepository

/**
 * Validation failures for the AccountRequest create path. Each variant carries
 * the data the resource needs to render Python's response-body shape.
 */
sealed class CreateAccountRequestResult {
  data class Created(val request: AccountRequest, val existingUser: UserDto?) : CreateAccountRequestResult()
  object SuperUserRejected : CreateAccountRequestResult()
  data class UserExists(val rolesForUser: List<MtpRole>) : CreateAccountRequestResult()
}

@Service
class AccountRequestService(
  private val accountRequestRepository: AccountRequestRepository,
  private val mtpUserRepository: MtpUserRepository,
  private val mtpRoleRepository: MtpRoleRepository,
  private val prisonRepository: PrisonRepository,
  private val prisonUserMappingRepository: PrisonUserMappingRepository,
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
   *
   * Mirrors Python's `AccountRequestViewSet.perform_create` validation:
   *   - if the username matches a superuser → SuperUserRejected
   *   - if the user already has a role AND `changeRole` is not "true" → UserExists
   *   - otherwise create the request (copying first_name/last_name/email from
   *     the existing user if any, matching Python's behaviour).
   */
  @Transactional
  fun createRequest(
    username: String,
    firstName: String,
    lastName: String,
    email: String,
    roleName: String?,
    prisonId: String?,
    changeRole: Boolean = false,
  ): CreateAccountRequestResult {
    val role = roleName?.let { mtpRoleRepository.findByName(it) }
    val prison = prisonId?.let { prisonRepository.findById(it).orElse(null) }
    val existingMtpUser = mtpUserRepository.findByUsernameIgnoreCase(username)

    if (existingMtpUser != null) {
      if (existingMtpUser.isSuperuser) {
        return CreateAccountRequestResult.SuperUserRejected
      }
      val rolesForUser = rolesFor(existingMtpUser)
      if (rolesForUser.isNotEmpty() && !changeRole) {
        return CreateAccountRequestResult.UserExists(rolesForUser)
      }
    }

    // Match Python: copy preserved details from existing user where present.
    val effectiveFirstName = existingMtpUser?.firstName ?: firstName
    val effectiveLastName = existingMtpUser?.lastName ?: lastName
    val effectiveEmail = existingMtpUser?.email ?: email

    val request = accountRequestRepository.save(
      AccountRequest().apply {
        this.username = username.lowercase()
        this.firstName = effectiveFirstName
        this.lastName = effectiveLastName
        this.email = effectiveEmail
        this.role = role
        this.prison = prison
      },
    )
    val existingUser = existingMtpUser?.let { UserDto.from(it, false) }
    return CreateAccountRequestResult.Created(request, existingUser)
  }

  /**
   * Returns the [MtpRole]s the user is in (via the role's `key_group`).
   * Mirrors Python's `Role.objects.get_roles_for_user`.
   */
  @Transactional(readOnly = true)
  fun rolesFor(user: MtpUser): List<MtpRole> {
    val groupIds = user.groups.mapNotNull { it.id }
    if (groupIds.isEmpty()) return emptyList()
    return mtpRoleRepository.findAll().filter { role -> role.keyGroup?.id in groupIds }
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

    val user = mtpUserRepository.findByUsernameIgnoreCase(request.username)?.also {
      it.isActive = true
      mtpUserRepository.save(it)
    } ?: run {
      val newUser = MtpUser().apply {
        this.username = request.username
        this.firstName = request.firstName
        this.lastName = request.lastName
        this.email = request.email
        this.isActive = true
      }
      mtpUserRepository.save(newUser)
    }
    // Mirror Python's `role.assign_to_user`: add the role's key_group (and
    // any other_groups) to the user's groups.
    request.role?.let { role ->
      val keyGroup = role.keyGroup
      if (keyGroup != null && user.groups.none { it.id == keyGroup.id }) {
        user.groups.add(keyGroup)
      }
      role.otherGroups.forEach { group ->
        if (user.groups.none { it.id == group.id }) user.groups.add(group)
      }
      mtpUserRepository.save(user)
    }
    // Mirror Python's `PrisonUserMapping.assign_prisons_to_user`: ensure a
    // mapping exists and that exactly the request's prison is associated.
    request.prison?.let { prison ->
      val mapping = prisonUserMappingRepository.findByUser(user)
        ?: MtpAuthPrisonusermapping().apply { this.user = user }
      mapping.prisons = mutableSetOf(prison)
      prisonUserMappingRepository.save(mapping)
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

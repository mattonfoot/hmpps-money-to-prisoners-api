package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpRole
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpRoleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository

@Service
class UserService(
  private val mtpUserRepository: MtpUserRepository,
  private val mtpRoleRepository: MtpRoleRepository,
  private val prisonRepository: PrisonRepository,
  private val loginTrackingService: LoginTrackingService,
) {

  /**
   * AUTH-010: Lists users, optionally filtered by role name or prison.
   */
  @Transactional(readOnly = true)
  fun listUsers(roleName: String? = null, prisonId: String? = null): List<Pair<MtpUser, Boolean>> {
    var spec: Specification<MtpUser> = Specification.where { _, _, cb -> cb.conjunction() }
    if (roleName != null) {
      spec = spec.and { root, _, cb -> cb.equal(root.join<MtpUser, MtpRole>("role").get<String>("name"), roleName) }
    }
    if (prisonId != null) {
      spec = spec.and { root, _, _ ->
        root.join<MtpUser, Prison>("prisons").get<String>("nomisId").`in`(prisonId)
      }
    }
    return mtpUserRepository.findAll(spec).map { user ->
      user to loginTrackingService.isLocked(user, "")
    }
  }

  /**
   * AUTH-011: Returns a user with their lock status, or null if not found.
   */
  @Transactional(readOnly = true)
  fun getUser(id: Long): Pair<MtpUser, Boolean>? {
    val user = mtpUserRepository.findById(id).orElse(null) ?: return null
    return user to loginTrackingService.isLocked(user, "")
  }

  /**
   * AUTH-012: Creates a new user.
   * AUTH-015: Rejects duplicate usernames (case-insensitive).
   * AUTH-016: Rejects duplicate emails.
   */
  @Transactional
  fun createUser(
    username: String,
    email: String,
    firstName: String? = null,
    lastName: String? = null,
    role: MtpRole?,
    prisons: Set<Prison>,
  ): MtpUser {
    if (mtpUserRepository.existsByUsernameIgnoreCase(username)) {
      throw IllegalArgumentException("A user with that username already exists")
    }
    if (email.isNotBlank() && mtpUserRepository.existsByEmailIgnoreCase(email)) {
      throw IllegalArgumentException("A user with that email already exists")
    }
    val user = MtpUser(
      username = username.lowercase(),
      email = email,
      firstName = firstName ?: "",
      lastName = lastName ?: "",
      role = role,
    )
    user.prisons = prisons.toMutableSet()
    return mtpUserRepository.save(user)
  }

  /**
   * AUTH-013: Partially updates a user.
   * AUTH-018: Role and prisons cannot be changed when [isSelf] is true.
   */
  @Transactional
  fun updateUser(
    id: Long,
    email: String?,
    firstName: String?,
    lastName: String?,
    prisons: Set<Prison>?,
    role: MtpRole?,
    isSelf: Boolean,
  ): MtpUser? {
    val user = mtpUserRepository.findById(id).orElse(null) ?: return null
    email?.let {
      if (it != user.email && it.isNotBlank() && mtpUserRepository.existsByEmailIgnoreCase(it)) {
        throw IllegalArgumentException("A user with that email already exists")
      }
      user.email = it
    }
    firstName?.let { user.firstName = it }
    lastName?.let { user.lastName = it }
    if (!isSelf) {
      prisons?.let { user.prisons = it.toMutableSet() }
      role?.let { user.role = it }
    }
    return mtpUserRepository.save(user)
  }

  /**
   * AUTH-014: Deactivates (soft-deletes) a user by setting is_active=false.
   */
  @Transactional
  fun deactivateUser(id: Long): MtpUser? {
    val user = mtpUserRepository.findById(id).orElse(null) ?: return null
    user.isActive = false
    return mtpUserRepository.save(user)
  }

  /**
   * AUTH-017: Unlocks an account by clearing all failed login attempts.
   */
  @Transactional
  fun unlockUser(id: Long): MtpUser? {
    val user = mtpUserRepository.findById(id).orElse(null) ?: return null
    loginTrackingService.unlockUser(user)
    return user
  }

  fun findById(id: Long): MtpUser? = mtpUserRepository.findById(id).orElse(null)

  fun findRoleByName(name: String?): MtpRole? = if (name == null) null else mtpRoleRepository.findByName(name)

  fun findPrisonsByIds(ids: List<String>): Set<Prison> = prisonRepository.findAllById(ids).toSet()
}

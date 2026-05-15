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
   *
   * `role` is not a real column on `auth_user`: Django assigns roles via
   * `auth_user_groups` → `mtp_auth_role.key_group_id`. To filter by role name
   * we resolve the role's key-group first and filter users by group membership.
   * Same story for prison — the user-prison link is `mtp_auth_prisonusermapping`.
   */
  @Transactional(readOnly = true)
  fun listUsers(roleName: String? = null, prisonId: String? = null): List<Pair<MtpUser, Boolean>> {
    var spec: Specification<MtpUser> = Specification.where { _, _, cb -> cb.conjunction() }
    if (roleName != null) {
      // No role with this name → empty result set (mirrors Python's queryset filter)
      val role = mtpRoleRepository.findByName(roleName)
      val keyGroupId = role?.keyGroup?.id
      spec = spec.and { root, _, cb ->
        if (keyGroupId == null) {
          cb.disjunction() // forces empty result set
        } else {
          val groupsJoin = root.join<Any, Any>("groups", jakarta.persistence.criteria.JoinType.INNER)
          cb.equal(groupsJoin.get<Long>("id"), keyGroupId)
        }
      }
    }
    if (prisonId != null) {
      spec = spec.and { root, _, cb ->
        val mappingJoin = root.join<Any, Any>("prisonUserMapping", jakarta.persistence.criteria.JoinType.INNER)
        val prisonsJoin = mappingJoin.join<Any, Any>("prisons", jakarta.persistence.criteria.JoinType.INNER)
        cb.equal(prisonsJoin.get<String>("nomisId"), prisonId)
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

  @Transactional(readOnly = true)
  fun getUserByUsername(username: String): Pair<MtpUser, Boolean>? {
    val user = mtpUserRepository.findByUsernameIgnoreCase(username) ?: return null
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
    val user = MtpUser().apply {
      this.username = username.lowercase()
      this.email = email
      this.firstName = firstName ?: ""
      this.lastName = lastName ?: ""
      this.isActive = true
    }
    val saved = mtpUserRepository.save(user)
    // Role assignment (group memberships) and prison-mapping assignment require
    // mtp_auth_role.other_groups + mtp_auth_prisonusermapping wiring. Stubbed
    // until the domain helpers are wired in.
    return saved
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
      // Django: a user-admin can change another user's role and prisons; the
      // user being edited cannot change their own (isSelf above prevents it).
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

  fun unlockUser(username: String): MtpUser? {
    val user = mtpUserRepository.findByUsernameIgnoreCase(username) ?: return null
    loginTrackingService.unlockUser(user)
    return user
  }

  fun findById(id: Long): MtpUser? = mtpUserRepository.findById(id).orElse(null)

  fun findByUsername(username: String): MtpUser? = mtpUserRepository.findByUsernameIgnoreCase(username)

  fun findRoleByName(name: String?): MtpRole? = if (name == null) null else mtpRoleRepository.findByName(name)

  fun findPrisonsByIds(ids: List<String>): Set<Prison> = prisonRepository.findAllById(ids).toSet()

  /**
   * Returns true when [requester] is permitted to GET/PATCH/DELETE [target] via
   * the `/users/{username}/` endpoints. Mirrors Python's
   * `mtp_auth/views.py::get_managed_user_queryset` + `UserViewSet.get_object`:
   *
   *   * the requester can always act on themselves;
   *   * superusers and users with ≠1 role key_group can only act on themselves;
   *   * otherwise the target must (a) share the same role key_group, and
   *     (b) share at least one prison from the requester's `PrisonUserMapping`
   *     (no prison filter for FIU requesters).
   */
  @Transactional(readOnly = true)
  fun canManage(requester: MtpUser, target: MtpUser): Boolean {
    if (requester.id == target.id) return true

    // Identify the requester's role key_group(s).
    val allRoles = mtpRoleRepository.findAll()
    val keyGroupIds = allRoles.mapNotNull { it.keyGroup?.id }.toSet()
    val requesterKeyGroups = requester.groups.mapNotNull { it.id }.filter { it in keyGroupIds }
    if (requesterKeyGroups.size != 1 || requester.isSuperuser) return false
    val requesterKeyGroup = requesterKeyGroups.single()

    // Target must share the same key_group and not be a superuser.
    if (target.isSuperuser) return false
    if (target.groups.none { it.id == requesterKeyGroup }) return false

    // FIU users skip the prison filter; others must overlap at least one prison.
    val isFiu = requester.groups.any { it.name == "FIU" }
    if (isFiu) return true

    val requesterPrisons = requester.prisonUserMapping?.prisons.orEmpty().mapNotNull { it.nomisId }.toSet()
    val targetPrisons = target.prisonUserMapping?.prisons.orEmpty().mapNotNull { it.nomisId }.toSet()
    if (requesterPrisons.isEmpty()) {
      // Requester has no prison set → target must also have no prison set.
      return targetPrisons.isEmpty()
    }
    return requesterPrisons.intersect(targetPrisons).isNotEmpty()
  }
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRule
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRuleState
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AutoAcceptRuleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

class SecurityAutoAcceptResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var autoAcceptRuleRepository: AutoAcceptRuleRepository

  @Autowired
  private lateinit var senderProfileRepository: SenderProfileRepository

  @Autowired
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  @BeforeEach
  fun setUp() {
    autoAcceptRuleRepository.deleteAll()
    senderProfileRepository.deleteAll()
    prisonerProfileRepository.deleteAll()
  }

  private fun createSenderProfile(): SenderProfile = senderProfileRepository.save(SenderProfile())

  private fun createPrisonerProfile(prisonerNumber: String = "A1234BC"): PrisonerProfile = prisonerProfileRepository.save(PrisonerProfile(prisonerNumber = prisonerNumber, prisonerName = "John Smith"))

  private fun createRule(sender: SenderProfile, prisoner: PrisonerProfile, active: Boolean = true, reason: String? = "Known sender"): AutoAcceptRule = transactionTemplate.execute {
    val rule = AutoAcceptRule(senderProfile = sender, prisonerProfile = prisoner)
    val state = AutoAcceptRuleState(rule = rule, active = active, reason = reason, createdBy = "test_user")
    rule.states.add(state)
    autoAcceptRuleRepository.save(rule)
  }!!

  @Nested
  @DisplayName("GET /security/checks/auto-accept/ (SEC-040 to SEC-047)")
  inner class ListAutoAcceptRules {

    @Test
    @DisplayName("SEC-040 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/security/checks/auto-accept/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-041 - Returns 403 for user without required role")
    fun `should return 403 without security role`() {
      webTestClient.get()
        .uri("/security/checks/auto-accept/")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("SEC-042 - Returns paginated list of auto-accept rules")
    fun `should return paginated list of rules`() {
      val sender = createSenderProfile()
      val prisoner = createPrisonerProfile()
      createRule(sender, prisoner)

      webTestClient.get()
        .uri("/security/checks/auto-accept/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results").isArray
        .jsonPath("$.results[0].sender_profile").isEqualTo(sender.id!!.toInt())
        .jsonPath("$.results[0].prisoner_profile").isEqualTo(prisoner.id!!.toInt())
        .jsonPath("$.results[0].is_active").isEqualTo(true)
    }

    @Test
    @DisplayName("SEC-043 - Returns empty list when no rules exist")
    fun `should return empty list when no rules`() {
      webTestClient.get()
        .uri("/security/checks/auto-accept/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("SEC-044 - Filters rules by is_active=true")
    fun `should filter by is_active true`() {
      val sender1 = createSenderProfile()
      val prisoner1 = createPrisonerProfile("A1234BC")
      val sender2 = createSenderProfile()
      val prisoner2 = createPrisonerProfile("B5678DE")
      createRule(sender1, prisoner1, active = true)
      createRule(sender2, prisoner2, active = false)

      webTestClient.get()
        .uri("/security/checks/auto-accept/?is_active=true")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].is_active").isEqualTo(true)
    }

    @Test
    @DisplayName("SEC-045 - Filters rules by is_active=false")
    fun `should filter by is_active false`() {
      val sender1 = createSenderProfile()
      val prisoner1 = createPrisonerProfile("A1234BC")
      val sender2 = createSenderProfile()
      val prisoner2 = createPrisonerProfile("B5678DE")
      createRule(sender1, prisoner1, active = true)
      createRule(sender2, prisoner2, active = false)

      webTestClient.get()
        .uri("/security/checks/auto-accept/?is_active=false")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].is_active").isEqualTo(false)
    }

    @Test
    @DisplayName("SEC-046 - Filters rules by sender_profile")
    fun `should filter by sender_profile`() {
      val sender1 = createSenderProfile()
      val sender2 = createSenderProfile()
      val prisoner1 = createPrisonerProfile("A1234BC")
      val prisoner2 = createPrisonerProfile("B5678DE")
      createRule(sender1, prisoner1)
      createRule(sender2, prisoner2)

      webTestClient.get()
        .uri("/security/checks/auto-accept/?sender_profile=${sender1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].sender_profile").isEqualTo(sender1.id!!.toInt())
    }

    @Test
    @DisplayName("SEC-047 - Filters rules by prisoner_profile")
    fun `should filter by prisoner_profile`() {
      val sender1 = createSenderProfile()
      val sender2 = createSenderProfile()
      val prisoner1 = createPrisonerProfile("A1234BC")
      val prisoner2 = createPrisonerProfile("B5678DE")
      createRule(sender1, prisoner1)
      createRule(sender2, prisoner2)

      webTestClient.get()
        .uri("/security/checks/auto-accept/?prisoner_profile=${prisoner2.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_profile").isEqualTo(prisoner2.id!!.toInt())
    }
  }

  @Nested
  @DisplayName("POST /security/checks/auto-accept/ (SEC-041)")
  inner class CreateAutoAcceptRule {

    @Test
    @DisplayName("SEC-041 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      val sender = createSenderProfile()
      val prisoner = createPrisonerProfile()

      webTestClient.post()
        .uri("/security/checks/auto-accept/")
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "sender_profile": ${sender.id},
            "prisoner_profile": ${prisoner.id},
            "states": [{"active": true, "reason": "Known sender"}]
          }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-041 - Creates auto-accept rule and returns 201")
    fun `should create auto-accept rule and return 201`() {
      val sender = createSenderProfile()
      val prisoner = createPrisonerProfile()

      webTestClient.post()
        .uri("/security/checks/auto-accept/")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "sender_profile": ${sender.id},
            "prisoner_profile": ${prisoner.id},
            "states": [{"active": true, "reason": "Known sender"}]
          }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.sender_profile").isEqualTo(sender.id!!.toInt())
        .jsonPath("$.prisoner_profile").isEqualTo(prisoner.id!!.toInt())
        .jsonPath("$.is_active").isEqualTo(true)
        .jsonPath("$.states[0].active").isEqualTo(true)
        .jsonPath("$.states[0].reason").isEqualTo("Known sender")
        .jsonPath("$.states[0].created_by").isEqualTo("security_user")

      assertThat(autoAcceptRuleRepository.count()).isEqualTo(1L)
    }

    @Test
    @DisplayName("SEC-041 - Returns 400 when rule already exists for sender/prisoner pair")
    fun `should return 400 for duplicate sender-prisoner pair`() {
      val sender = createSenderProfile()
      val prisoner = createPrisonerProfile()
      createRule(sender, prisoner)

      webTestClient.post()
        .uri("/security/checks/auto-accept/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "sender_profile": ${sender.id},
            "prisoner_profile": ${prisoner.id},
            "states": [{"active": true, "reason": "Duplicate"}]
          }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("SEC-041 - Returns 403 for user without required role")
    fun `should return 403 without security role`() {
      val sender = createSenderProfile()
      val prisoner = createPrisonerProfile()

      webTestClient.post()
        .uri("/security/checks/auto-accept/")
        .headers(setAuthorisation(roles = listOf()))
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "sender_profile": ${sender.id},
            "prisoner_profile": ${prisoner.id},
            "states": [{"active": true}]
          }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  @DisplayName("PATCH /security/checks/auto-accept/{id}/ (SEC-044)")
  inner class PatchAutoAcceptRule {

    @Test
    @DisplayName("SEC-044 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      val sender = createSenderProfile()
      val prisoner = createPrisonerProfile()
      val rule = createRule(sender, prisoner, active = true)

      webTestClient.patch()
        .uri("/security/checks/auto-accept/${rule.id}/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"states": [{"active": false, "reason": "Revoked"}]}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-044 - Appends new state to existing rule, returns 200")
    fun `should append new state and reflect updated is_active`() {
      val sender = createSenderProfile()
      val prisoner = createPrisonerProfile()
      val rule = createRule(sender, prisoner, active = true, reason = "Initially active")

      webTestClient.patch()
        .uri("/security/checks/auto-accept/${rule.id}/")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"states": [{"active": false, "reason": "Revoked"}]}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.is_active").isEqualTo(false)
        .jsonPath("$.states.length()").isEqualTo(2)
        .jsonPath("$.states[1].active").isEqualTo(false)
        .jsonPath("$.states[1].reason").isEqualTo("Revoked")
        .jsonPath("$.states[1].created_by").isEqualTo("security_user")
    }

    @Test
    @DisplayName("SEC-044 - Can re-activate a deactivated rule")
    fun `should re-activate rule by appending active=true state`() {
      val sender = createSenderProfile()
      val prisoner = createPrisonerProfile()
      val rule = createRule(sender, prisoner, active = false, reason = "Initially inactive")

      webTestClient.patch()
        .uri("/security/checks/auto-accept/${rule.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"states": [{"active": true, "reason": "Re-activated"}]}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.is_active").isEqualTo(true)
    }

    @Test
    @DisplayName("SEC-044 - Returns 404 for non-existent rule")
    fun `should return 404 for non-existent rule`() {
      webTestClient.patch()
        .uri("/security/checks/auto-accept/99999/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"states": [{"active": false}]}""")
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    @DisplayName("SEC-044 - Returns 400 when states list is empty")
    fun `should return 400 when states list is empty`() {
      val sender = createSenderProfile()
      val prisoner = createPrisonerProfile()
      val rule = createRule(sender, prisoner)

      webTestClient.patch()
        .uri("/security/checks/auto-accept/${rule.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"states": []}""")
        .exchange()
        .expectStatus().isBadRequest
    }
  }
}

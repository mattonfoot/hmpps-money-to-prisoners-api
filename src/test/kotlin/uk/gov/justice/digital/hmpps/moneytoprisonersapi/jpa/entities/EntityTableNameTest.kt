package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Table
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Schema alignment tests: every JPA entity must map to its corresponding Django production
 * table so the Kotlin service can run against the existing Django-managed PostgreSQL database.
 *
 * Django uses the naming convention  {app_label}_{model_name_lowercase}.
 * These tests are the single source of truth for the expected table name for each entity.
 * Add a new entry here whenever a new @Entity is introduced.
 */
@DisplayName("Entity → Django table name alignment")
class EntityTableNameTest {

  @ParameterizedTest(name = "{1} must map entity {0} to Django table")
  @MethodSource("entityTableMappings")
  fun `entity maps to correct Django table name`(entityClass: Class<*>, expectedTableName: String) {
    val tableAnnotation = entityClass.getAnnotation(Table::class.java)
      ?: error("${entityClass.simpleName} has no @Table annotation — add @Table(name = \"$expectedTableName\")")
    assertEquals(
      expectedTableName,
      tableAnnotation.name,
      "${entityClass.simpleName}: expected Django table '$expectedTableName' but @Table maps to '${tableAnnotation.name}'",
    )
  }

  companion object {
    @JvmStatic
    fun entityTableMappings(): Stream<Arguments> = Stream.of(
      // ── prison app ──────────────────────────────────────────────────────────────
      Arguments.of(Prison::class.java, "prison_prison"),
      Arguments.of(PrisonCategory::class.java, "prison_category"),
      Arguments.of(PrisonPopulation::class.java, "prison_population"),
      Arguments.of(PrisonerLocation::class.java, "prison_prisonerlocation"),
      Arguments.of(PrisonerBalance::class.java, "prison_prisonerbalance"),
      Arguments.of(PrisonerCreditNoticeEmail::class.java, "prison_prisonercreditnoticeemail"),

      // ── credit app ──────────────────────────────────────────────────────────────
      Arguments.of(Credit::class.java, "credit_credit"),
      Arguments.of(Comment::class.java, "credit_comment"),
      Arguments.of(Batch::class.java, "credit_processingbatch"),
      Arguments.of(PrivateEstateBatch::class.java, "credit_privateestatebatch"),

      // ── credit audit log (Kotlin-specific, no direct Django equivalent — named
      //    following Django {app}_{model} convention for the credit app)
      Arguments.of(Log::class.java, "credit_log"),

      // ── transaction app ─────────────────────────────────────────────────────────
      Arguments.of(Transaction::class.java, "transaction_transaction"),

      // ── payment app ─────────────────────────────────────────────────────────────
      Arguments.of(Payment::class.java, "payment_payment"),
      Arguments.of(BillingAddress::class.java, "payment_billingaddress"),

      // ── disbursement app ────────────────────────────────────────────────────────
      Arguments.of(Disbursement::class.java, "disbursement_disbursement"),
      Arguments.of(DisbursementLog::class.java, "disbursement_log"),
      Arguments.of(DisbursementComment::class.java, "disbursement_comment"),

      // ── security app ────────────────────────────────────────────────────────────
      Arguments.of(SenderProfile::class.java, "security_senderprofile"),
      Arguments.of(PrisonerProfile::class.java, "security_prisonerprofile"),
      Arguments.of(SecurityCheck::class.java, "security_check"),
      Arguments.of(AutoAcceptRule::class.java, "security_checkautoacceptrule"),
      Arguments.of(AutoAcceptRuleState::class.java, "security_checkautoacceptrulestate"),
      Arguments.of(SavedSearch::class.java, "security_savedsearch"),
      Arguments.of(MonitoredPartialEmailAddress::class.java, "security_monitoredpartialemailaddress"),

      // ── notification app ────────────────────────────────────────────────────────
      Arguments.of(Event::class.java, "notification_event"),
      Arguments.of(EmailNotificationPreferences::class.java, "notification_emailnotificationpreferences"),

      // ── core app ────────────────────────────────────────────────────────────────
      Arguments.of(ScheduledCommand::class.java, "core_scheduledcommand"),

      // ── performance app ─────────────────────────────────────────────────────────
      Arguments.of(DigitalTakeup::class.java, "performance_digitaltakeup"),
      Arguments.of(UserSatisfaction::class.java, "performance_usersatisfaction"),

      // ── mtp_auth app ────────────────────────────────────────────────────────────
      Arguments.of(MtpRole::class.java, "mtp_auth_role"),
      Arguments.of(MtpUserLogin::class.java, "mtp_auth_login"),
      Arguments.of(AccountRequest::class.java, "mtp_auth_accountrequest"),
      Arguments.of(FailedLoginAttempt::class.java, "mtp_auth_failedloginattempt"),
      Arguments.of(JobInformation::class.java, "mtp_auth_jobinformation"),
    )
  }
}

package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Table
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Schema alignment tests: every JPA entity must map to its corresponding
 * production table as defined in the V1__create_database.sql dump.
 *
 * These tests are the single source of truth for the expected table name for each entity.
 * Add a new entry here whenever a new @Entity is introduced.
 */
@DisplayName("Entity → database table name alignment")
class EntityTableNameTest {

  @ParameterizedTest(name = "{1} must map entity {0} to database table")
  @MethodSource("entityTableMappings")
  fun `entity maps to correct database table name`(entityClass: Class<*>, expectedTableName: String) {
    val tableAnnotation = entityClass.getAnnotation(Table::class.java)
      ?: error("${entityClass.simpleName} has no @Table annotation — add @Table(name = \"$expectedTableName\")")
    assertEquals(
      expectedTableName,
      tableAnnotation.name,
      "${entityClass.simpleName}: expected table '$expectedTableName' but @Table maps to '${tableAnnotation.name}'",
    )
  }

  companion object {
    @JvmStatic
    fun entityTableMappings(): Stream<Arguments> = Stream.of(
      // ── account ─────────────────────────────────────────────────────────────────
      Arguments.of(Balance::class.java, "balances"),

      // ── core ────────────────────────────────────────────────────────────────────
      Arguments.of(ScheduledCommand::class.java, "core_scheduledcommand"),
      Arguments.of(FileDownload::class.java, "file_downloads"),

      // ── credit ──────────────────────────────────────────────────────────────────
      Arguments.of(Credit::class.java, "credit_credit"),
      Arguments.of(Comment::class.java, "credit_comment"),
      Arguments.of(Log::class.java, "credit_log"),
      Arguments.of(Batch::class.java, "credit_processingbatch"),
      Arguments.of(PrivateEstateBatch::class.java, "credit_privateestatebatch"),

      // ── transaction ─────────────────────────────────────────────────────────────
      Arguments.of(Transaction::class.java, "transaction_transaction"),

      // ── payment ─────────────────────────────────────────────────────────────────
      Arguments.of(Payment::class.java, "payment_payment"),
      Arguments.of(BillingAddress::class.java, "payment_billingaddress"),
      Arguments.of(PaymentBatch::class.java, "payment_batches"),

      // ── disbursement ────────────────────────────────────────────────────────────
      Arguments.of(Disbursement::class.java, "disbursement_disbursement"),
      Arguments.of(DisbursementLog::class.java, "disbursement_log"),
      Arguments.of(DisbursementComment::class.java, "disbursement_comment"),

      // ── prison ──────────────────────────────────────────────────────────────────
      Arguments.of(Prison::class.java, "prison_prison"),
      Arguments.of(PrisonCategory::class.java, "prison_category"),
      Arguments.of(PrisonPopulation::class.java, "prison_population"),
      Arguments.of(PrisonerLocation::class.java, "prison_prisonerlocation"),
      Arguments.of(PrisonerBalance::class.java, "prison_prisonerbalance"),
      Arguments.of(PrisonerCreditNoticeEmail::class.java, "prison_prisonercreditnoticeemail"),

      // ── security ────────────────────────────────────────────────────────────────
      Arguments.of(SenderProfile::class.java, "security_senderprofile"),
      Arguments.of(PrisonerProfile::class.java, "security_prisonerprofile"),
      Arguments.of(RecipientProfile::class.java, "security_recipientprofile"),
      Arguments.of(SecurityCheck::class.java, "security_check"),
      Arguments.of(AutoAcceptRule::class.java, "security_checkautoacceptrule"),
      Arguments.of(AutoAcceptRuleState::class.java, "security_checkautoacceptrulestate"),
      Arguments.of(SavedSearch::class.java, "security_savedsearch"),
      Arguments.of(MonitoredPartialEmailAddress::class.java, "security_monitoredpartialemailaddress"),

      // ── notification ────────────────────────────────────────────────────────────
      Arguments.of(Event::class.java, "notification_event"),
      Arguments.of(EmailNotificationPreferences::class.java, "notification_emailnotificationpreferences"),

      // ── service ─────────────────────────────────────────────────────────────────
      Arguments.of(Downtime::class.java, "service_downtime"),
      Arguments.of(ServiceNotification::class.java, "service_notification"),

      // ── performance ─────────────────────────────────────────────────────────────
      Arguments.of(DigitalTakeup::class.java, "performance_digitaltakeup"),
      Arguments.of(UserSatisfaction::class.java, "performance_usersatisfaction"),
      Arguments.of(PerformanceData::class.java, "performance_performancedata"),

      // ── mtp_auth ────────────────────────────────────────────────────────────────
      Arguments.of(MtpRole::class.java, "mtp_auth_role"),
      Arguments.of(MtpUser::class.java, "mtp_users"),
      Arguments.of(MtpUserLogin::class.java, "mtp_auth_login"),
      Arguments.of(AccountRequest::class.java, "mtp_auth_accountrequest"),
      Arguments.of(FailedLoginAttempt::class.java, "mtp_auth_failedloginattempt"),
      Arguments.of(JobInformation::class.java, "mtp_auth_jobinformation"),
      Arguments.of(PasswordResetToken::class.java, "password_reset_tokens"),
      Arguments.of(UserFlag::class.java, "user_flags"),

      // ── user_event_log ──────────────────────────────────────────────────────────
      Arguments.of(UserEvent::class.java, "user_events"),
    )
  }
}

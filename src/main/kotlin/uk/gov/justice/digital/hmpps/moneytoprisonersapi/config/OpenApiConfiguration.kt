package uk.gov.justice.digital.hmpps.moneytoprisonersapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Tag name constants matching the Python API's tag names exactly.
// Order follows the Python API's alphabetical convention.
const val TAG_BALANCES = "balances"
const val TAG_BATCHES = "batches"
const val TAG_CHANGE_PASSWORD = "change_password"
const val TAG_CREDITS = "credits"
const val TAG_DISBURSEMENTS = "disbursements"
const val TAG_EMAIL_PREFERENCES = "emailpreferences"
const val TAG_EVENTS = "events"
const val TAG_FILE_DOWNLOADS = "file-downloads"
const val TAG_JOB_INFORMATION = "job-information"
const val TAG_MONITORED = "monitored"
const val TAG_NOTIFICATIONS = "notifications"
const val TAG_PAYMENTS = "payments"
const val TAG_PERFORMANCE = "performance"
const val TAG_PRISON_CATEGORIES = "prison_categories"
const val TAG_PRISON_POPULATIONS = "prison_populations"
const val TAG_PRISONER_ACCOUNT_BALANCES = "prisoner_account_balances"
const val TAG_PRISONER_CREDIT_NOTICE_EMAIL = "prisoner_credit_notice_email"
const val TAG_PRISONER_LOCATIONS = "prisoner_locations"
const val TAG_PRISONER_VALIDITY = "prisoner_validity"
const val TAG_PRISONERS = "prisoners"
const val TAG_PRISONS = "prisons"
const val TAG_PRIVATE_ESTATE_BATCHES = "private-estate-batches"
const val TAG_RECIPIENTS = "recipients"
const val TAG_REQUESTS = "requests"
const val TAG_RESET_PASSWORD = "reset_password"
const val TAG_ROLES = "roles"
const val TAG_RULES = "rules"
const val TAG_SEARCHES = "searches"
const val TAG_SECURITY = "security"
const val TAG_SENDERS = "senders"
const val TAG_TRANSACTIONS = "transactions"
const val TAG_USERS = "users"

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version!!

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://money-to-prisoners-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://money-to-prisoners-api-preprod.hmpps.service.justice.gov.uk").description("Pre-Production"),
        Server().url("https://money-to-prisoners-api.hmpps.service.justice.gov.uk").description("Production"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .tags(
      listOf(
        Tag().name(TAG_BALANCES).description("Daily prisoner account balance snapshots"),
        Tag().name(TAG_BATCHES).description("Credit processing batch management"),
        Tag().name(TAG_CHANGE_PASSWORD).description("Authenticated password change"),
        Tag().name(TAG_CREDITS).description("Prisoner credits, comments, and credit actions"),
        Tag().name(TAG_DISBURSEMENTS).description("Prisoner disbursement management and lifecycle actions"),
        Tag().name(TAG_EMAIL_PREFERENCES).description("User email notification preferences"),
        Tag().name(TAG_EVENTS).description("Notification event log"),
        Tag().name(TAG_FILE_DOWNLOADS).description("File download tracking"),
        Tag().name(TAG_JOB_INFORMATION).description("Job information submitted alongside account requests"),
        Tag().name(TAG_MONITORED).description("Monitored sender/prisoner counts and monitored email keywords"),
        Tag().name(TAG_NOTIFICATIONS).description("Banner and alert notifications shown in front-end applications"),
        Tag().name(TAG_PAYMENTS).description("Online prisoner money payments (debit card)"),
        Tag().name(TAG_PERFORMANCE).description("Weekly performance and reporting data"),
        Tag().name(TAG_PRISON_CATEGORIES).description("Prison category definitions"),
        Tag().name(TAG_PRISON_POPULATIONS).description("Prison population type definitions"),
        Tag().name(TAG_PRISONER_ACCOUNT_BALANCES).description("NOMIS prisoner account balance lookups"),
        Tag().name(TAG_PRISONER_CREDIT_NOTICE_EMAIL).description("Prisoner credit notice email configurations per prison"),
        Tag().name(TAG_PRISONER_LOCATIONS).description("Prisoner location management and bulk upload"),
        Tag().name(TAG_PRISONER_VALIDITY).description("Prisoner number validation against NOMIS"),
        Tag().name(TAG_PRISONERS).description("Prisoner security profiles, monitoring, and credit/disbursement history"),
        Tag().name(TAG_PRISONS).description("Prison data management"),
        Tag().name(TAG_PRIVATE_ESTATE_BATCHES).description("Private estate credit batch management"),
        Tag().name(TAG_RECIPIENTS).description("Recipient security profiles, monitoring, and disbursement history"),
        Tag().name(TAG_REQUESTS).description("Self-service account request workflow"),
        Tag().name(TAG_RESET_PASSWORD).description("Unauthenticated password reset via email token"),
        Tag().name(TAG_ROLES).description("MTP role definitions"),
        Tag().name(TAG_RULES).description("Auto-accept rules for security checks"),
        Tag().name(TAG_SEARCHES).description("User-specific saved search management"),
        Tag().name(TAG_SECURITY).description("Security checks on credits"),
        Tag().name(TAG_SENDERS).description("Sender security profiles, monitoring, and credit history"),
        Tag().name(TAG_TRANSACTIONS).description("Bank transfer transaction management"),
        Tag().name(TAG_USERS).description("MTP user accounts, flags, and role management"),
      ),
    )
    .info(
      Info().title("HMPPS Money to Prisoners API").version(version)
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
    .components(
      Components().addSecuritySchemes(
        "oauth2_provider",
        SecurityScheme()
          .type(SecurityScheme.Type.OAUTH2)
          .description("test")
          .flows(
            OAuthFlows().password(
              OAuthFlow()
                .tokenUrl("/oauth2/token/")
                .authorizationUrl("/oauth2/authorize/")
                .scopes(Scopes().addString("read", "Read scope").addString("write", "Write scope")),
            ),
          ),
      ),
    )
    .addSecurityItem(
      SecurityRequirement().addList("oauth2_provider"),
    )

  /**
   * Removes the auto-generated `PaginatedResponse*` schemas from the OpenAPI spec
   * and replaces all `$ref` references to them with the inline schema definition.
   * The Python API does not expose these as named types — pagination uses inline objects.
   */
  @Bean
  fun hidePaginatedResponseSchemas(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
    val components = openApi.components ?: return@OpenApiCustomizer
    val schemas = components.schemas ?: return@OpenApiCustomizer
    val paginatedSchemas = schemas.filterKeys { it.startsWith("PaginatedResponse") }
    if (paginatedSchemas.isEmpty()) return@OpenApiCustomizer

    // Build inline replacements (deep copies of the schema bodies) keyed by ref path
    val replacements = paginatedSchemas.mapKeys { (name, _) -> "#/components/schemas/$name" }

    // Walk all operations and replace $ref to PaginatedResponse* with the inline schema
    openApi.paths?.values?.forEach { pathItem ->
      pathItem.readOperations().forEach { op ->
        op.responses?.values?.forEach { response ->
          response.content?.values?.forEach { mediaType ->
            mediaType.schema?.let { schema ->
              val ref = schema.`$ref`
              if (ref != null && replacements.containsKey(ref)) {
                val inline = replacements[ref]!!
                schema.`$ref` = null
                schema.type = inline.type
                schema.properties = inline.properties
                schema.required = inline.required
              }
            }
          }
        }
      }
    }

    // Now remove the PaginatedResponse* schemas
    paginatedSchemas.keys.forEach { schemas.remove(it) }
  }
}

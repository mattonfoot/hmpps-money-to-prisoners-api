package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

/**
 * Defines the enabled notification rules exposed via GET /rules/.
 * NOT-008: Only enabled rules (MONP and MONS) are returned.
 */
data class NotificationRule(val code: String, val description: String)

val ENABLED_RULE_CODES = setOf("MONP", "MONS")

val RULES: Map<String, NotificationRule> = mapOf(
  "MONP" to NotificationRule(
    code = "MONP",
    description = "Credits or disbursements for prisoners you are monitoring",
  ),
  "MONS" to NotificationRule(
    code = "MONS",
    description = "Credits for payment sources you are monitoring",
  ),
  "MONR" to NotificationRule(
    code = "MONR",
    description = "Disbursements for recipients you are monitoring",
  ),
  "FIUMONP" to NotificationRule(
    code = "FIUMONP",
    description = "Credits or disbursements for FIU prisoners",
  ),
  "FIUMONS" to NotificationRule(
    code = "FIUMONS",
    description = "Credits for FIU payment sources",
  ),
  "FIUMONR" to NotificationRule(
    code = "FIUMONR",
    description = "Disbursements for FIU recipients",
  ),
  "FIUMONE" to NotificationRule(
    code = "FIUMONE",
    description = "Payment source is using a monitored keyword in the email address",
  ),
  "NWN" to NotificationRule(
    code = "NWN",
    description = "Credits or disbursements that are not a whole number",
  ),
  "HA" to NotificationRule(
    code = "HA",
    description = "Credits or disbursements over £120.00",
  ),
  "CSYM" to NotificationRule(
    code = "CSYM",
    description = "Credits from debit cards or bank accounts with symbols in their name",
  ),
  "CSFREQ" to NotificationRule(
    code = "CSFREQ",
    description = "More than 3 credits from the same debit card or bank account to any prisoner in a week",
  ),
  "DRFREQ" to NotificationRule(
    code = "DRFREQ",
    description = "More than 4 disbursements from any prisoner to the same bank account in 4 weeks",
  ),
  "CSNUM" to NotificationRule(
    code = "CSNUM",
    description = "A prisoner getting money from more than 3 debit cards or bank accounts in a week",
  ),
  "DRNUM" to NotificationRule(
    code = "DRNUM",
    description = "A prisoner sending money to more than 4 bank accounts in 4 weeks",
  ),
  "CPNUM" to NotificationRule(
    code = "CPNUM",
    description = "A debit card or bank account sending money to more than 3 prisoners in a week",
  ),
  "DPNUM" to NotificationRule(
    code = "DPNUM",
    description = "A bank account getting money from more than 4 prisoners in 4 weeks",
  ),
)

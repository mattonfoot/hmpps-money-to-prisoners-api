package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import java.net.InetAddress
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

// Top-level "fake constructor" functions, in the same package as the typealiases
// in Aliases.kt. These exist so test code that pre-dates the IntelliJ regen can
// continue to call e.g. `Credit(id = 1L, amount = 100)` without rewriting every
// call site to the post-regen `Credit().apply { ... }` form.

private fun stubPrison(nomisId: String?): PrisonPrison? = nomisId?.let { PrisonPrison().apply { this.nomisId = it } }
private fun stubUser(username: String?): AuthUser? = username?.let { AuthUser().apply { this.username = it } }

@Suppress("FunctionName")
fun Credit(
  id: Long? = null,
  amount: Long = 0L,
  prisonerNumber: String? = null,
  prisonerName: String? = null,
  prisonerDob: LocalDate? = null,
  prison: Any? = null,
  resolution: Any? = null,
  blocked: Boolean = false,
  reviewed: Boolean = false,
  reconciled: Boolean = false,
  receivedAt: Any? = null,
  owner: Any? = null,
  incompleteSenderInfo: Boolean = false,
  nomisTransactionId: String? = null,
): CreditCredit = CreditCredit().apply {
  this.id = id
  this.amount = amount
  this.prisonerNumber = prisonerNumber
  this.prisonerName = prisonerName
  this.prisonerDob = prisonerDob
  this.prison = when (prison) {
    is PrisonPrison -> prison
    is String -> stubPrison(prison)
    else -> null
  }
  this.resolution = when (resolution) {
    is CreditResolution -> resolution.value
    is String -> resolution
    null -> ""
    else -> resolution.toString()
  }
  this.blocked = blocked
  this.reviewed = reviewed
  this.reconciled = reconciled
  this.receivedAt = when (receivedAt) {
    is OffsetDateTime -> receivedAt
    is LocalDateTime -> receivedAt.atOffset(ZoneOffset.UTC)
    else -> null
  }
  this.owner = when (owner) {
    is AuthUser -> owner
    is String -> stubUser(owner)
    else -> null
  }
  this.nomisTransactionId = nomisTransactionId
}

@Suppress("FunctionName")
fun Disbursement(
  id: Long? = null,
  amount: Long = 0L,
  prisonerNumber: String = "",
  prisonerName: String = "",
  prison: Any? = null,
  recipientFirstName: String = "",
  recipientLastName: String = "",
  recipientIsCompany: Boolean = false,
  recipientEmail: String? = null,
  sortCode: String = "",
  accountNumber: String = "",
  rollNumber: String? = null,
  method: Any = "",
  invoiceNumber: String? = null,
  nomisTransactionId: String? = null,
  resolution: Any? = null,
  remittanceDescription: String = "",
  // Django doesn't store sender postcode on disbursement_disbursement; the
  // legacy domain put it on the recipient address. Accepted here for legacy
  // test signatures and ignored.
  @Suppress("UNUSED_PARAMETER") postcode: String? = null,
): DisbursementDisbursement = DisbursementDisbursement().apply {
  this.id = id
  this.amount = amount.toInt()
  this.prisonerNumber = prisonerNumber
  this.prisonerName = prisonerName
  this.prison = when (prison) {
    is PrisonPrison -> prison
    is String -> stubPrison(prison)
    else -> null
  }
  this.recipientFirstName = recipientFirstName
  this.recipientLastName = recipientLastName
  this.recipientIsCompany = recipientIsCompany
  this.recipientEmail = recipientEmail
  this.sortCode = sortCode
  this.accountNumber = accountNumber
  this.rollNumber = rollNumber
  this.method = when (method) {
    is DisbursementMethod -> method.value
    is String -> method
    else -> method.toString()
  }
  this.invoiceNumber = invoiceNumber
  this.nomisTransactionId = nomisTransactionId
  this.resolution = when (resolution) {
    is DisbursementResolution -> resolution.value
    is String -> resolution
    null -> ""
    else -> resolution.toString()
  }
  this.remittanceDescription = remittanceDescription
}

@Suppress("FunctionName")
fun Transaction(
  id: Long? = null,
  amount: Long = 0L,
  prisonerNumber: String = "",
  prisonerName: String = "",
  prison: Any? = null,
  category: Any? = null,
  source: Any? = null,
  reference: String = "",
  senderName: String? = "",
  senderSortCode: String? = "",
  senderAccountNumber: String? = "",
  senderRollNumber: String? = "",
  referenceInSenderField: Boolean = false,
  incompleteSenderInfo: Boolean = false,
  processorTypeCode: String? = null,
  credit: CreditCredit? = null,
  receivedAt: Any? = null,
): TransactionTransaction = TransactionTransaction().apply {
  this.id = id
  this.amount = amount
  this.category = when (category) {
    is TransactionCategory -> category.value
    is String -> category
    null -> ""
    else -> category.toString()
  }
  this.source = when (source) {
    is TransactionSource -> source.value
    is String -> source
    null -> ""
    else -> source.toString()
  }
  this.reference = reference
  this.senderName = senderName ?: ""
  this.senderSortCode = senderSortCode ?: ""
  this.senderAccountNumber = senderAccountNumber ?: ""
  this.senderRollNumber = senderRollNumber ?: ""
  this.referenceInSenderField = referenceInSenderField
  this.incompleteSenderInfo = incompleteSenderInfo
  this.processorTypeCode = processorTypeCode
  this.credit = credit
  if (this.credit != null) {
    if (prisonerNumber.isNotEmpty()) this.credit!!.prisonerNumber = prisonerNumber
    if (prisonerName.isNotEmpty()) this.credit!!.prisonerName = prisonerName
    val ra: OffsetDateTime? = when (receivedAt) {
      is OffsetDateTime -> receivedAt
      is LocalDateTime -> receivedAt.atOffset(ZoneOffset.UTC)
      else -> null
    }
    if (ra != null) this.credit!!.receivedAt = ra
  }
  // prison parameter ignored — Transaction has no direct prison FK; lives on credit.
  @Suppress("UNUSED_VARIABLE")
  val ignoredPrison = prison
}

@Suppress("FunctionName")
fun Payment(
  uuid: UUID? = null,
  amount: Number = 0,
  status: String = "pending",
  cardholderName: String? = null,
  cardNumberFirstDigits: String? = null,
  cardNumberLastDigits: String? = null,
  cardExpiryDate: String? = null,
  cardBrand: String? = null,
  ipAddress: Any? = null,
  billingAddress: PaymentBillingaddress? = null,
  credit: CreditCredit? = null,
  // Django doesn't model an `email` field directly on payment_payment; the
  // legacy DTO surfaced it via the billing address. Accepted here as a
  // legacy-compat parameter and ignored.
  @Suppress("UNUSED_PARAMETER") email: String? = null,
): PaymentPayment = PaymentPayment().apply {
  this.uuid = uuid
  this.amount = amount.toInt()
  this.status = status
  this.cardholderName = cardholderName
  this.cardNumberFirstDigits = cardNumberFirstDigits
  this.cardNumberLastDigits = cardNumberLastDigits
  this.cardExpiryDate = cardExpiryDate
  this.cardBrand = cardBrand
  this.ipAddress = when (ipAddress) {
    is InetAddress -> ipAddress
    is String -> InetAddress.getByName(ipAddress)
    else -> null
  }
  this.billingAddress = billingAddress
  this.credit = credit
}

@Suppress("FunctionName")
fun BillingAddress(
  line1: String? = null,
  line2: String? = null,
  city: String? = null,
  country: String? = null,
  postcode: String? = null,
): PaymentBillingaddress = PaymentBillingaddress().apply {
  this.line1 = line1
  this.line2 = line2
  this.city = city
  this.country = country
  this.postcode = postcode
}

@Suppress("FunctionName")
fun Prison(
  nomisId: String = "",
  name: String = "",
  region: String = "",
  privateEstate: Boolean = false,
): PrisonPrison = PrisonPrison().apply {
  this.nomisId = nomisId
  this.name = name
  this.region = region
  this.privateEstate = privateEstate
}

@Suppress("FunctionName")
fun PrisonerLocation(
  id: Long? = null,
  prisonerNumber: String = "",
  prison: Any? = null,
  active: Boolean = true,
  createdBy: Any? = null,
  prisonerDob: LocalDate? = null,
): PrisonPrisonerlocation = PrisonPrisonerlocation().apply {
  this.id = id
  this.prisonerNumber = prisonerNumber
  this.prison = when (prison) {
    is PrisonPrison -> prison
    is String -> stubPrison(prison)
    else -> null
  }
  this.active = active
  this.createdBy = when (createdBy) {
    is AuthUser -> createdBy
    is String -> stubUser(createdBy)
    else -> null
  }
  this.prisonerDob = prisonerDob ?: LocalDate.now()
}

@Suppress("FunctionName")
fun MtpUser(
  id: Long? = null,
  username: String = "",
  email: String = "",
  firstName: String = "",
  lastName: String = "",
  role: MtpAuthRole? = null,
): AuthUser = AuthUser().apply {
  this.id = id
  this.username = username
  this.email = email
  this.firstName = firstName
  this.lastName = lastName
  this.role = role
}

@Suppress("FunctionName")
fun MtpRole(
  id: Long? = null,
  name: String = "",
  keyGroup: Any? = null,
  application: Any? = null,
  @Suppress("UNUSED_PARAMETER") otherGroups: Any? = null,
): MtpAuthRole = MtpAuthRole().apply {
  this.id = id
  this.name = name
  this.keyGroup = when (keyGroup) {
    is AuthGroup -> keyGroup
    is String -> AuthGroup().apply { this.name = keyGroup }
    else -> null
  }
  this.application = when (application) {
    is Oauth2ProviderApplication -> application
    is String -> Oauth2ProviderApplication().apply { clientId = application; this.name = application }
    else -> null
  }
}

@Suppress("FunctionName")
fun ServiceNotification(
  target: String = "",
  level: Int = 0,
  public: Boolean = false,
  headline: String = "",
  message: String = "",
  start: Any? = null,
  end: Any? = null,
): ServiceNotification = ServiceNotification().apply {
  this.target = target
  this.level = level.toShort()
  this.publicField = public
  this.headline = headline
  this.message = message
  this.start = when (start) {
    is OffsetDateTime -> start
    is LocalDateTime -> start.atOffset(ZoneOffset.UTC)
    else -> OffsetDateTime.now()
  }
  this.end = when (end) {
    is OffsetDateTime -> end
    is LocalDateTime -> end.atOffset(ZoneOffset.UTC)
    else -> null
  }
}

@Suppress("FunctionName")
fun Downtime(
  service: String = "",
  start: Any? = null,
  end: Any? = null,
  messageToUsers: String = "",
): ServiceDowntime = ServiceDowntime().apply {
  this.service = service
  this.start = when (start) {
    is OffsetDateTime -> start
    is LocalDateTime -> start.atOffset(ZoneOffset.UTC)
    else -> OffsetDateTime.now()
  }
  this.end = when (end) {
    is OffsetDateTime -> end
    is LocalDateTime -> end.atOffset(ZoneOffset.UTC)
    else -> null
  }
  this.messageToUsers = messageToUsers
}

@Suppress("FunctionName")
fun ScheduledCommand(
  id: Long? = null,
  name: String = "",
  argString: String = "",
  cronEntry: String = "",
  nextExecution: Any? = null,
  deleteAfterNext: Boolean = false,
): CoreScheduledcommand = CoreScheduledcommand().apply {
  this.id = id
  this.name = name
  this.argString = argString
  this.cronEntry = cronEntry
  this.nextExecution = when (nextExecution) {
    is OffsetDateTime -> nextExecution
    is LocalDateTime -> nextExecution.atOffset(ZoneOffset.UTC)
    else -> null
  }
  this.deleteAfterNext = deleteAfterNext
}

@Suppress("FunctionName")
fun Comment(
  id: Long? = null,
  comment: String = "",
  user: AuthUser? = null,
  userId: String? = null,
  credit: CreditCredit? = null,
  @Suppress("UNUSED_PARAMETER") category: Any? = null,
): CreditComment = CreditComment().apply {
  this.id = id
  this.comment = comment
  this.user = user ?: stubUser(userId)
  this.credit = credit
}

@Suppress("FunctionName")
fun Log(
  id: Long? = null,
  action: Any? = null,
  user: AuthUser? = null,
  userId: String? = null,
  credit: CreditCredit? = null,
  created: OffsetDateTime? = null,
): CreditLog = CreditLog().apply {
  this.id = id
  this.action = when (action) {
    is LogAction -> action.value
    is String -> action
    null -> ""
    else -> action.toString()
  }
  this.user = user ?: stubUser(userId)
  this.credit = credit
  if (created != null) this.created = created
}

@Suppress("FunctionName")
fun Balance(
  id: Long? = null,
  closingBalance: Any = 0L,
  date: LocalDate = LocalDate.now(),
  @Suppress("UNUSED_PARAMETER") prison: Any? = null,
): AccountBalance = AccountBalance().apply {
  this.id = id
  this.closingBalance = when (closingBalance) {
    is Long -> closingBalance
    is Int -> closingBalance.toLong()
    is java.math.BigInteger -> closingBalance.toLong()
    is Number -> closingBalance.toLong()
    else -> 0L
  }
  this.date = date
}

@Suppress("FunctionName")
fun PrivateEstateBatch(
  id: Long? = null,
  @Suppress("UNUSED_PARAMETER") ref: String? = null,
  prison: Any? = null,
  date: LocalDate = LocalDate.now(),
  @Suppress("UNUSED_PARAMETER") totalAmount: Long = 0L,
): CreditPrivateestatebatch = CreditPrivateestatebatch().apply {
  this.id = id
  this.prison = when (prison) {
    is PrisonPrison -> prison
    is String -> stubPrison(prison)
    else -> null
  }
  this.date = date
}

@Suppress("FunctionName")
fun Batch(
  id: Long? = null,
  @Suppress("UNUSED_PARAMETER") refCode: String? = null,
  @Suppress("UNUSED_PARAMETER") date: LocalDate = LocalDate.now(),
  user: AuthUser? = null,
  credits: Collection<CreditCredit>? = null,
): CreditProcessingbatch = CreditProcessingbatch().apply {
  this.id = id
  if (user != null) this.user = user
  if (credits != null) {
    this.credits = credits.toMutableSet()
  }
}

@Suppress("FunctionName")
fun DigitalTakeup(
  id: Long? = null,
  date: LocalDate = LocalDate.now(),
  prison: Any? = null,
  creditsByMtp: Int = 0,
  creditsByPost: Int = 0,
  amountByMtp: Int? = null,
  amountByPost: Int? = null,
): PerformanceDigitaltakeup = PerformanceDigitaltakeup().apply {
  this.id = id
  this.date = date
  this.prison = when (prison) {
    is PrisonPrison -> prison
    is String -> stubPrison(prison)
    else -> null
  }
  this.creditsByMtp = creditsByMtp
  this.creditsByPost = creditsByPost
  this.amountByMtp = amountByMtp
  this.amountByPost = amountByPost
}

@Suppress("FunctionName")
fun PerformanceData(
  id: LocalDate = LocalDate.now(),
  digitalTakeup: Double? = null,
  completionRate: Double? = null,
  userSatisfaction: Double? = null,
  @Suppress("UNUSED_PARAMETER") rated1: Int? = null,
  @Suppress("UNUSED_PARAMETER") rated2: Int? = null,
  @Suppress("UNUSED_PARAMETER") rated3: Int? = null,
  @Suppress("UNUSED_PARAMETER") rated4: Int? = null,
  @Suppress("UNUSED_PARAMETER") rated5: Int? = null,
  creditsTotal: Int? = null,
  creditsByMtp: Int? = null,
): PerformancePerformancedatum = PerformancePerformancedatum().apply {
  this.id = id
  this.digitalTakeup = digitalTakeup
  this.completionRate = completionRate
  this.userSatisfaction = userSatisfaction
  this.creditsTotal = creditsTotal
  this.creditsByMtp = creditsByMtp
}

@Suppress("FunctionName")
fun MtpUserLogin(
  id: Long? = null,
  user: AuthUser? = null,
  application: Oauth2ProviderApplication? = null,
): MtpAuthLogin = MtpAuthLogin().apply {
  this.id = id
  if (user != null) this.user = user
  this.application = application
}

@Suppress("FunctionName")
fun FailedLoginAttempt(
  id: Long? = null,
  user: AuthUser? = null,
  application: Oauth2ProviderApplication? = null,
): MtpAuthFailedloginattempt = MtpAuthFailedloginattempt().apply {
  this.id = id
  if (user != null) this.user = user
  this.application = application
}

@Suppress("FunctionName")
fun JobInformation(
  id: Long? = null,
  name: String = "",
  @Suppress("UNUSED_PARAMETER") startedAt: Any? = null,
  user: AuthUser? = null,
  prisonEstate: String = "",
  tasks: String = "",
): MtpAuthJobinformation = MtpAuthJobinformation().apply {
  this.id = id
  this.title = name
  this.prisonEstate = prisonEstate
  this.tasks = tasks
  if (user != null) this.user = user
}

@Suppress("FunctionName")
fun PasswordChangeRequest(
  uuid: UUID? = null,
  user: AuthUser? = null,
  created: OffsetDateTime? = null,
): MtpAuthPasswordchangerequest = MtpAuthPasswordchangerequest().apply {
  this.id = uuid
  if (user != null) this.user = user
  if (created != null) this.created = created
}

@Suppress("FunctionName")
fun AccountRequest(
  id: Long? = null,
  username: String = "",
  firstName: String = "",
  lastName: String = "",
  email: String = "",
  role: MtpAuthRole? = null,
  prison: Any? = null,
  reason: String = "",
  manager_email: String? = null,
): MtpAuthAccountrequest = MtpAuthAccountrequest().apply {
  this.id = id
  this.username = username
  this.firstName = firstName
  this.lastName = lastName
  this.email = email
  this.role = role
  this.prison = when (prison) {
    is PrisonPrison -> prison
    is String -> stubPrison(prison)
    else -> null
  }
  this.reason = reason
  this.managerEmail = manager_email
}

@Suppress("FunctionName")
fun FileDownload(
  id: Long? = null,
  label: String = "",
  date: LocalDate = LocalDate.now(),
): CoreFiledownload = CoreFiledownload().apply {
  this.id = id
  this.label = label
  this.date = date
}

@Suppress("FunctionName")
fun UserSatisfaction(
  @Suppress("UNUSED_PARAMETER") id: Long? = null,
  date: LocalDate = LocalDate.now(),
  rated1: Int = 0,
  rated2: Int = 0,
  rated3: Int = 0,
  rated4: Int = 0,
  rated5: Int = 0,
): PerformanceUsersatisfaction = PerformanceUsersatisfaction().apply {
  this.id = date
  this.rated1 = rated1
  this.rated2 = rated2
  this.rated3 = rated3
  this.rated4 = rated4
  this.rated5 = rated5
}

@Suppress("FunctionName")
fun OAuthApplication(
  id: Long = 0L,
  clientId: String = "",
  name: String = "",
): Oauth2ProviderApplication = Oauth2ProviderApplication().apply {
  this.id = id
  this.clientId = clientId
  this.name = name
}

@Suppress("FunctionName")
fun OAuthAccessToken(
  id: Long = 0L,
  token: String = "",
  expires: OffsetDateTime = OffsetDateTime.now().plusHours(1),
  user: AuthUser? = null,
  application: Oauth2ProviderApplication? = null,
): Oauth2ProviderAccesstoken = Oauth2ProviderAccesstoken().apply {
  this.id = id
  this.token = token
  this.expires = expires
  if (user != null) this.user = user
  this.application = application
}

@Suppress("FunctionName")
fun SenderProfile(
  id: Long? = null,
  @Suppress("UNUSED_PARAMETER") prisonerCount: Long = 0L,
  creditCount: Long = 0L,
  creditTotal: Long = 0L,
): SecuritySenderprofile = SecuritySenderprofile().apply {
  this.id = id
  this.creditCount = creditCount
  this.creditTotal = creditTotal
}

@Suppress("FunctionName")
fun PrisonerProfile(
  id: Long? = null,
  prisonerNumber: String = "",
  prisonerName: String = "",
  @Suppress("UNUSED_PARAMETER") senderCount: Long = 0L,
  creditCount: Long = 0L,
  creditTotal: Long = 0L,
): SecurityPrisonerprofile = SecurityPrisonerprofile().apply {
  this.id = id
  this.prisonerNumber = prisonerNumber
  this.prisonerName = prisonerName
  this.creditCount = creditCount
  this.creditTotal = creditTotal
}

@Suppress("FunctionName")
fun RecipientProfile(
  id: Long? = null,
  @Suppress("UNUSED_PARAMETER") prisonerCount: Long = 0L,
  disbursementCount: Long = 0L,
  disbursementTotal: Long = 0L,
  // Django moves sortCode/accountNumber onto the per-detail child
  // (security_banktransferrecipientdetail). Accepted here for legacy test
  // signatures; the values are not stored on the profile itself.
  @Suppress("UNUSED_PARAMETER") sortCode: String = "",
  @Suppress("UNUSED_PARAMETER") accountNumber: String = "",
): SecurityRecipientprofile = SecurityRecipientprofile().apply {
  this.id = id
  this.disbursementCount = disbursementCount
  this.disbursementTotal = disbursementTotal
}

@Suppress("FunctionName")
fun Event(
  id: Long? = null,
  rule: String = "",
  description: String = "",
  triggeredAt: Any? = null,
  user: AuthUser? = null,
  username: String? = null,
  // Tests sometimes pass `credit` to associate via the per-kind subevent.
  // Accepted here for legacy compat — the actual subevent row needs to be
  // created and linked separately.
  @Suppress("UNUSED_PARAMETER") credit: CreditCredit? = null,
): NotificationEvent = NotificationEvent().apply {
  this.id = id
  this.rule = rule
  this.description = description
  this.triggeredAt = when (triggeredAt) {
    is OffsetDateTime -> triggeredAt
    is LocalDateTime -> triggeredAt.atOffset(ZoneOffset.UTC)
    else -> OffsetDateTime.now()
  }
  this.user = user ?: stubUser(username)
}

@Suppress("FunctionName")
fun UserEvent(
  id: Long = 0L,
  data: Map<String, Any>? = null,
  @Suppress("UNUSED_PARAMETER") endpoint: String = "",
  @Suppress("UNUSED_PARAMETER") method: String = "",
  apiUrlPath: String = "",
  kind: String = "",
): UserEventLogUserevent = UserEventLogUserevent().apply {
  this.id = id
  this.data = data
  this.apiUrlPath = apiUrlPath
  this.kind = kind
}

@Suppress("FunctionName")
fun SavedSearch(
  id: Long? = null,
  description: String = "",
  endpoint: String = "",
  @Suppress("UNUSED_PARAMETER") filters: Any? = null,
  user: AuthUser? = null,
  username: String? = null,
): SecuritySavedsearch = SecuritySavedsearch().apply {
  this.id = id
  this.description = description
  this.endpoint = endpoint
  this.user = user ?: stubUser(username) ?: AuthUser()
}

@Suppress("FunctionName")
fun UserFlag(
  id: Long? = null,
  name: String = "",
  user: AuthUser? = null,
): MtpAuthFlag = MtpAuthFlag().apply {
  this.id = id
  this.name = name
  if (user != null) this.user = user
}

@Suppress("FunctionName")
fun Flag(
  id: Long? = null,
  name: String = "",
  user: AuthUser? = null,
): MtpAuthFlag = MtpAuthFlag().apply {
  this.id = id
  this.name = name
  if (user != null) this.user = user
}

@Suppress("FunctionName")
fun EmailNotificationPreferences(
  id: Long? = null,
  user: AuthUser? = null,
  username: String? = null,
  frequency: Any = "",
): NotificationEmailnotificationpreference = NotificationEmailnotificationpreference().apply {
  this.id = id
  this.user = user ?: stubUser(username)
  this.frequency = when (frequency) {
    is String -> frequency
    is Enum<*> -> frequency.name.lowercase()
    else -> frequency.toString()
  }
}

@Suppress("FunctionName")
fun PrisonerCreditNoticeEmail(
  id: Long? = null,
  email: String = "",
  prison: Any? = null,
): PrisonPrisonercreditnoticeemail = PrisonPrisonercreditnoticeemail().apply {
  this.id = id
  this.email = email
  this.prison = when (prison) {
    is PrisonPrison -> prison
    is String -> stubPrison(prison) ?: PrisonPrison()
    else -> PrisonPrison()
  }
}

@Suppress("FunctionName")
fun PrisonerBalance(
  @Suppress("UNUSED_PARAMETER") id: Long? = null,
  prisonerNumber: String = "",
  prison: Any? = null,
  amount: Long = 0L,
): PrisonPrisonerbalance = PrisonPrisonerbalance().apply {
  this.prisonerNumber = prisonerNumber
  this.prison = when (prison) {
    is PrisonPrison -> prison
    is String -> stubPrison(prison)
    else -> null
  }
  this.amount = amount
}

@Suppress("FunctionName")
fun MonitoredPartialEmailAddress(
  id: Long? = null,
  keyword: String = "",
): SecurityMonitoredpartialemailaddress = SecurityMonitoredpartialemailaddress().apply {
  this.id = id
  this.keyword = keyword
}

@Suppress("FunctionName")
fun PrisonCategory(
  id: Long? = null,
  name: String = "",
): PrisonCategory = PrisonCategory().apply {
  this.id = id
  this.name = name
}

@Suppress("FunctionName")
fun PrisonPopulation(
  id: Long? = null,
  name: String = "",
): PrisonPopulation = PrisonPopulation().apply {
  this.id = id
  this.name = name
}

@Suppress("FunctionName")
fun PasswordResetToken(
  uuid: UUID? = null,
  user: AuthUser? = null,
): MtpAuthPasswordchangerequest = MtpAuthPasswordchangerequest().apply {
  this.id = uuid
  if (user != null) this.user = user
}

@Suppress("FunctionName")
fun SecurityCheck(
  id: Long? = null,
  status: Any? = null,
  description: Any? = null,
  decisionReason: String = "",
  actionedBy: Any? = null,
  actionedAt: OffsetDateTime? = null,
  credit: CreditCredit? = null,
  rules: Any? = null,
  rejectionReasons: Map<String, Any>? = null,
  // The legacy domain stored per-rule codes as a separate list and the
  // started-at timestamp on the check; Django models them as `rules`/`actioned_at`.
  ruleCodes: Any? = null,
  startedAt: Any? = null,
): SecurityCheck = SecurityCheck().apply {
  this.id = id
  this.status = when (status) {
    is CheckStatus -> status.value
    is String -> status
    null -> ""
    else -> status.toString()
  }
  this.description = description
  this.decisionReason = decisionReason
  this.actionedBy = when (actionedBy) {
    is AuthUser -> actionedBy
    is String -> stubUser(actionedBy)
    else -> null
  }
  this.actionedAt = actionedAt
  this.credit = credit
  this.rules = rules ?: ruleCodes
  this.rejectionReasons = rejectionReasons
  val startedAtOffset = when (startedAt) {
    is OffsetDateTime -> startedAt
    is LocalDateTime -> startedAt.atOffset(ZoneOffset.UTC)
    else -> null
  }
  if (startedAtOffset != null) this.actionedAt = this.actionedAt ?: startedAtOffset
}

@Suppress("FunctionName")
fun AutoAcceptRule(
  id: Long? = null,
  // Django keys the rule off `debit_card_sender_details_id`, not the sender
  // profile directly. The legacy `senderProfile` param is accepted but
  // ignored — tests asserting auto-accept against a sender need to seed a
  // detail child that points at the senderProfile.
  @Suppress("UNUSED_PARAMETER") senderProfile: Any? = null,
  prisonerProfile: SecurityPrisonerprofile? = null,
): SecurityCheckautoacceptrule = SecurityCheckautoacceptrule().apply {
  this.id = id
  this.prisonerProfile = prisonerProfile
}

@Suppress("FunctionName")
fun AutoAcceptRuleState(
  id: Long? = null,
  active: Boolean = true,
  reason: String? = null,
  addedBy: Any? = null,
  createdBy: Any? = null,
  created: OffsetDateTime? = null,
  autoAcceptRule: SecurityCheckautoacceptrule? = null,
  rule: SecurityCheckautoacceptrule? = null,
): SecurityCheckautoacceptrulestate = SecurityCheckautoacceptrulestate().apply {
  this.id = id
  this.active = active
  this.reason = reason ?: ""
  this.addedBy = when {
    addedBy is AuthUser -> addedBy
    addedBy is String -> stubUser(addedBy)
    createdBy is AuthUser -> createdBy
    createdBy is String -> stubUser(createdBy)
    else -> null
  }
  if (created != null) this.created = created
  this.autoAcceptRule = autoAcceptRule ?: rule
}

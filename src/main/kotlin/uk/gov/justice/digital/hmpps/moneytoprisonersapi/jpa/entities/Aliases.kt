package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

// Bridges old domain-class names to the IntelliJ-regenerated entity names.
// These let the existing services/repos continue to refer to e.g. `Credit`,
// `Disbursement`, `Balance` etc. while the underlying entity files follow
// Django's `<app>_<model>` table-derived naming.

typealias Credit = CreditCredit
typealias Disbursement = DisbursementDisbursement
typealias Balance = AccountBalance
typealias Comment = CreditComment
typealias Log = CreditLog
typealias Transaction = TransactionTransaction
typealias Payment = PaymentPayment
typealias BillingAddress = PaymentBillingaddress
typealias Batch = CreditProcessingbatch
typealias PrivateEstateBatch = CreditPrivateestatebatch
typealias AutoAcceptRule = SecurityCheckautoacceptrule
typealias AutoAcceptRuleState = SecurityCheckautoacceptrulestate
typealias Prison = PrisonPrison
typealias PrisonerLocation = PrisonPrisonerlocation
typealias PrisonerBalance = PrisonPrisonerbalance
typealias PrisonerCreditNoticeEmail = PrisonPrisonercreditnoticeemail
typealias MtpUser = AuthUser
typealias MtpRole = MtpAuthRole
typealias MtpUserLogin = MtpAuthLogin
typealias SenderProfile = SecuritySenderprofile
typealias PrisonerProfile = SecurityPrisonerprofile
typealias RecipientProfile = SecurityRecipientprofile
typealias MonitoredPartialEmailAddress = SecurityMonitoredpartialemailaddress
typealias SavedSearch = SecuritySavedsearch
typealias AccountRequest = MtpAuthAccountrequest
typealias Downtime = ServiceDowntime
typealias FileDownload = CoreFiledownload
typealias DigitalTakeup = PerformanceDigitaltakeup
typealias ScheduledCommand = CoreScheduledcommand
typealias FailedLoginAttempt = MtpAuthFailedloginattempt
typealias JobInformation = MtpAuthJobinformation
typealias PrisonUserMapping = MtpAuthPrisonusermapping
typealias ApplicationUserMapping = MtpAuthApplicationusermapping
typealias Flag = MtpAuthFlag
typealias EmailNotificationPreferences = NotificationEmailnotificationpreference
typealias Event = NotificationEvent
typealias PasswordChangeRequest = MtpAuthPasswordchangerequest
typealias OAuthAccessToken = Oauth2ProviderAccesstoken
typealias OAuthApplication = Oauth2ProviderApplication
typealias UserFlag = MtpAuthFlag
typealias PasswordResetToken = MtpAuthPasswordchangerequest
typealias UserEvent = UserEventLogUserevent
typealias PerformanceData = PerformancePerformancedatum
typealias UserSatisfaction = PerformanceUsersatisfaction

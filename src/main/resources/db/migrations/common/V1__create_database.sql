--
-- Name: balances; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.balances (
    balance_id integer NOT NULL,
    closing_balance bigint NOT NULL,
    date date NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: balances_balance_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.balances_balance_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: balances_balance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.balances_balance_id_seq OWNED BY public.balances.balance_id;

--
-- Name: core_scheduledcommand; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.core_scheduledcommand (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    arg_string character varying(500) DEFAULT ''::character varying NOT NULL,
    cron_entry character varying(255) NOT NULL,
    next_execution timestamp without time zone,
    delete_after_next boolean DEFAULT false NOT NULL
);

--
-- Name: core_scheduledcommand_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.core_scheduledcommand_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: core_scheduledcommand_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.core_scheduledcommand_id_seq OWNED BY public.core_scheduledcommand.id;

--
-- Name: credit_comment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.credit_comment (
    comment_id integer NOT NULL,
    comment text NOT NULL,
    credit_id integer,
    user_id character varying(255),
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: credit_comment_comment_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.credit_comment_comment_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: credit_comment_comment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.credit_comment_comment_id_seq OWNED BY public.credit_comment.comment_id;

--
-- Name: credit_credit; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.credit_credit (
    credit_id integer NOT NULL,
    amount bigint NOT NULL,
    prisoner_number character varying(250),
    prisoner_name character varying(250),
    prisoner_dob date,
    prison character varying(10),
    resolution character varying(50) DEFAULT 'PENDING'::character varying NOT NULL,
    reconciled boolean DEFAULT false NOT NULL,
    reviewed boolean DEFAULT false NOT NULL,
    blocked boolean DEFAULT false NOT NULL,
    received_at timestamp without time zone,
    owner character varying(255),
    nomis_transaction_id character varying(50),
    source character varying(50) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    incomplete_sender_info boolean DEFAULT false NOT NULL
);

--
-- Name: credit_credit_credit_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.credit_credit_credit_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: credit_credit_credit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.credit_credit_credit_id_seq OWNED BY public.credit_credit.credit_id;

--
-- Name: credit_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.credit_log (
    log_id integer NOT NULL,
    action character varying(50) NOT NULL,
    credit_id integer,
    user_id character varying(255),
    created timestamp without time zone NOT NULL
);

--
-- Name: credit_log_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.credit_log_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: credit_log_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.credit_log_log_id_seq OWNED BY public.credit_log.log_id;

--
-- Name: credit_privateestatebatch; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.credit_privateestatebatch (
    ref character varying(30) NOT NULL,
    prison character varying(10) NOT NULL,
    date date NOT NULL,
    total_amount bigint DEFAULT 0 NOT NULL,
    created timestamp without time zone NOT NULL
);

--
-- Name: credit_privateestatebatch_credits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.credit_privateestatebatch_credits (
    ref character varying(30) NOT NULL,
    credit_id integer NOT NULL
);

--
-- Name: credit_processingbatch; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.credit_processingbatch (
    batch_id integer NOT NULL,
    owner character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL
);

--
-- Name: credit_processingbatch_batch_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.credit_processingbatch_batch_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: credit_processingbatch_batch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.credit_processingbatch_batch_id_seq OWNED BY public.credit_processingbatch.batch_id;

--
-- Name: credit_processingbatch_credits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.credit_processingbatch_credits (
    batch_id integer NOT NULL,
    credit_id integer NOT NULL
);

--
-- Name: disbursement_comment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.disbursement_comment (
    disbursement_comment_id integer NOT NULL,
    comment text NOT NULL,
    category character varying(100),
    disbursement_id integer,
    user_id character varying(255),
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: disbursement_comment_disbursement_comment_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.disbursement_comment_disbursement_comment_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: disbursement_comment_disbursement_comment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.disbursement_comment_disbursement_comment_id_seq OWNED BY public.disbursement_comment.disbursement_comment_id;

--
-- Name: disbursement_disbursement; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.disbursement_disbursement (
    disbursement_id integer NOT NULL,
    amount bigint NOT NULL,
    method character varying(50) NOT NULL,
    prison character varying(10),
    prisoner_number character varying(250),
    prisoner_name character varying(250),
    recipient_first_name character varying(250),
    recipient_last_name character varying(250),
    recipient_email character varying(254),
    address_line1 character varying(250),
    address_line2 character varying(250),
    city character varying(250),
    postcode character varying(250),
    country character varying(250),
    sort_code character varying(50),
    account_number character varying(50),
    roll_number character varying(50),
    recipient_is_company boolean DEFAULT false NOT NULL,
    resolution character varying(50) DEFAULT 'PENDING'::character varying NOT NULL,
    nomis_transaction_id character varying(50),
    invoice_number character varying(50),
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: disbursement_disbursement_disbursement_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.disbursement_disbursement_disbursement_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: disbursement_disbursement_disbursement_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.disbursement_disbursement_disbursement_id_seq OWNED BY public.disbursement_disbursement.disbursement_id;

--
-- Name: disbursement_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.disbursement_log (
    disbursement_log_id integer NOT NULL,
    action character varying(50) NOT NULL,
    disbursement_id integer,
    user_id character varying(255),
    created timestamp without time zone NOT NULL
);

--
-- Name: disbursement_log_disbursement_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.disbursement_log_disbursement_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: disbursement_log_disbursement_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.disbursement_log_disbursement_log_id_seq OWNED BY public.disbursement_log.disbursement_log_id;

--
-- Name: file_downloads; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.file_downloads (
    id integer NOT NULL,
    label character varying(255) NOT NULL,
    date date NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: file_downloads_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.file_downloads_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: file_downloads_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.file_downloads_id_seq OWNED BY public.file_downloads.id;

--
--
-- Name: mtp_auth_accountrequest; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mtp_auth_accountrequest (
    id integer NOT NULL,
    username character varying(150) NOT NULL,
    first_name character varying(150) DEFAULT ''::character varying NOT NULL,
    last_name character varying(150) DEFAULT ''::character varying NOT NULL,
    email character varying(254) DEFAULT ''::character varying NOT NULL,
    role_id integer,
    prison_nomis_id character varying(10),
    status character varying(20) DEFAULT 'pending'::character varying NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: mtp_auth_accountrequest_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mtp_auth_accountrequest_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: mtp_auth_accountrequest_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.mtp_auth_accountrequest_id_seq OWNED BY public.mtp_auth_accountrequest.id;

--
-- Name: mtp_auth_failedloginattempt; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mtp_auth_failedloginattempt (
    id integer NOT NULL,
    user_id integer NOT NULL,
    application character varying(50) DEFAULT ''::character varying NOT NULL,
    attempted_at timestamp without time zone NOT NULL
);

--
-- Name: mtp_auth_failedloginattempt_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mtp_auth_failedloginattempt_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: mtp_auth_failedloginattempt_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.mtp_auth_failedloginattempt_id_seq OWNED BY public.mtp_auth_failedloginattempt.id;

--
-- Name: mtp_auth_jobinformation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mtp_auth_jobinformation (
    id integer NOT NULL,
    user_id integer NOT NULL,
    title character varying(255) DEFAULT ''::character varying NOT NULL,
    prison_estate character varying(255) DEFAULT ''::character varying NOT NULL,
    tasks text DEFAULT ''::text NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: mtp_auth_jobinformation_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mtp_auth_jobinformation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: mtp_auth_jobinformation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.mtp_auth_jobinformation_id_seq OWNED BY public.mtp_auth_jobinformation.id;

--
-- Name: mtp_auth_login; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mtp_auth_login (
    id integer NOT NULL,
    user_id integer NOT NULL,
    application character varying(50) DEFAULT ''::character varying NOT NULL,
    logged_in_at timestamp without time zone NOT NULL
);

--
-- Name: mtp_auth_login_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mtp_auth_login_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: mtp_auth_login_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.mtp_auth_login_id_seq OWNED BY public.mtp_auth_login.id;

--
-- Name: mtp_auth_prisonusermapping_prisons; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mtp_auth_prisonusermapping_prisons (
    user_id integer NOT NULL,
    prison_nomis_id character varying(10) NOT NULL
);

--
-- Name: mtp_auth_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mtp_auth_role (
    id integer NOT NULL,
    name character varying(150) NOT NULL,
    key_group character varying(150) DEFAULT ''::character varying NOT NULL,
    other_groups text DEFAULT ''::text NOT NULL,
    application character varying(50) DEFAULT ''::character varying NOT NULL
);

--
-- Name: mtp_auth_role_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mtp_auth_role_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: mtp_auth_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.mtp_auth_role_id_seq OWNED BY public.mtp_auth_role.id;

--
-- Name: mtp_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mtp_users (
    id integer NOT NULL,
    username character varying(150) NOT NULL,
    email character varying(254) DEFAULT ''::character varying NOT NULL,
    first_name character varying(150) DEFAULT ''::character varying NOT NULL,
    last_name character varying(150) DEFAULT ''::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    role_id integer,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: mtp_users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mtp_users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: mtp_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.mtp_users_id_seq OWNED BY public.mtp_users.id;

--
-- Name: notification_emailnotificationpreferences; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notification_emailnotificationpreferences (
    id integer NOT NULL,
    username character varying(250) NOT NULL,
    frequency character varying(50) NOT NULL,
    last_sent_at date
);

--
-- Name: notification_emailnotificationpreferences_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.notification_emailnotificationpreferences_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: notification_emailnotificationpreferences_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.notification_emailnotificationpreferences_id_seq OWNED BY public.notification_emailnotificationpreferences.id;

--
-- Name: notification_event; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notification_event (
    id integer NOT NULL,
    rule character varying(8) NOT NULL,
    description character varying(500) DEFAULT ''::character varying NOT NULL,
    triggered_at timestamp without time zone,
    username character varying(250),
    credit_id bigint,
    disbursement_id bigint,
    sender_profile_id bigint,
    prisoner_profile_id bigint
);

--
-- Name: notification_event_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.notification_event_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: notification_event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.notification_event_id_seq OWNED BY public.notification_event.id;

--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.password_reset_tokens (
    id integer NOT NULL,
    user_id integer NOT NULL,
    token uuid NOT NULL,
    application character varying(50) DEFAULT ''::character varying NOT NULL,
    created_at timestamp without time zone NOT NULL,
    used boolean DEFAULT false NOT NULL
);

--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.password_reset_tokens_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.password_reset_tokens_id_seq OWNED BY public.password_reset_tokens.id;

--
-- Name: payment_batch_credits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payment_batch_credits (
    payment_batch_id integer NOT NULL,
    credit_id integer NOT NULL
);

--
-- Name: payment_batches; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payment_batches (
    payment_batch_id integer NOT NULL,
    ref_code integer NOT NULL,
    settlement_date date,
    created timestamp without time zone NOT NULL
);

--
-- Name: payment_batches_payment_batch_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.payment_batches_payment_batch_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: payment_batches_payment_batch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.payment_batches_payment_batch_id_seq OWNED BY public.payment_batches.payment_batch_id;

--
-- Name: payment_billingaddress; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payment_billingaddress (
    billing_address_id integer NOT NULL,
    line1 character varying(250),
    line2 character varying(250),
    city character varying(250),
    country character varying(250),
    postcode character varying(250),
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: payment_billingaddress_billing_address_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.payment_billingaddress_billing_address_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: payment_billingaddress_billing_address_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.payment_billingaddress_billing_address_id_seq OWNED BY public.payment_billingaddress.billing_address_id;

--
-- Name: payment_payment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payment_payment (
    uuid uuid NOT NULL,
    amount bigint DEFAULT 0 NOT NULL,
    service_charge bigint DEFAULT 0 NOT NULL,
    status character varying(50),
    processor_id character varying(250),
    recipient_name character varying(250),
    email character varying(254),
    cardholder_name character varying(250),
    card_number_first_digits character varying(6),
    card_number_last_digits character varying(4),
    card_expiry_date character varying(5),
    card_brand character varying(250),
    ip_address character varying(45),
    credit_id integer NOT NULL,
    billing_address_id integer,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: prison_category; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prison_category (
    category_id integer NOT NULL,
    name character varying(255) NOT NULL
);

--
-- Name: prison_category_category_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.prison_category_category_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: prison_category_category_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.prison_category_category_id_seq OWNED BY public.prison_category.category_id;

--
-- Name: prison_population; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prison_population (
    population_id integer NOT NULL,
    name character varying(255) NOT NULL
);

--
-- Name: prison_population_population_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.prison_population_population_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: prison_population_population_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.prison_population_population_id_seq OWNED BY public.prison_population.population_id;

--
-- Name: prison_prison; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prison_prison (
    nomis_id character varying(10) NOT NULL,
    name character varying(255) DEFAULT ''::character varying NOT NULL,
    region character varying(255) DEFAULT ''::character varying NOT NULL,
    pre_approval_required boolean DEFAULT false NOT NULL,
    private_estate boolean DEFAULT false NOT NULL,
    use_nomis_for_balances boolean DEFAULT true NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: prison_prison_categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prison_prison_categories (
    prison_nomis_id character varying(10) NOT NULL,
    category_id integer NOT NULL
);

--
-- Name: prison_prison_populations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prison_prison_populations (
    prison_nomis_id character varying(10) NOT NULL,
    population_id integer NOT NULL
);

--
-- Name: prison_prisonerbalance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prison_prisonerbalance (
    id integer NOT NULL,
    prisoner_number character varying(250) NOT NULL,
    prison_id character varying(10) NOT NULL,
    amount bigint DEFAULT 0 NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: prison_prisonerbalance_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.prison_prisonerbalance_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: prison_prisonerbalance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.prison_prisonerbalance_id_seq OWNED BY public.prison_prisonerbalance.id;

--
-- Name: prison_prisonercreditnoticeemail; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prison_prisonercreditnoticeemail (
    prison_id character varying(10) NOT NULL,
    email character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: prison_prisonerlocation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prison_prisonerlocation (
    id integer NOT NULL,
    prisoner_number character varying(250) NOT NULL,
    prison_id character varying(10) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_by character varying(250) NOT NULL,
    prisoner_dob date,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: prison_prisonerlocation_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.prison_prisonerlocation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: prison_prisonerlocation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.prison_prisonerlocation_id_seq OWNED BY public.prison_prisonerlocation.id;

--
-- Name: prisoner_profile_credits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prisoner_profile_credits (
    prisoner_profile_id integer NOT NULL,
    credit_id integer NOT NULL
);

--
-- Name: prisoner_profile_monitoring_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prisoner_profile_monitoring_users (
    prisoner_profile_id integer NOT NULL,
    user_id character varying(255) NOT NULL
);

--
-- Name: recipient_profile_monitoring_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.recipient_profile_monitoring_users (
    recipient_profile_id bigint NOT NULL,
    user_id character varying(255) NOT NULL
);

--
-- Name: security_check; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_check (
    check_id integer NOT NULL,
    status character varying(50) DEFAULT 'PENDING'::character varying NOT NULL,
    description text,
    decision_reason text,
    actioned_by character varying(255),
    actioned_at timestamp without time zone,
    credit_id integer NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    rule_codes text,
    descriptions text,
    rejection_reasons text,
    started_at timestamp without time zone,
    assigned_to character varying(255)
);

--
-- Name: security_check_check_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.security_check_check_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: security_check_check_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.security_check_check_id_seq OWNED BY public.security_check.check_id;

--
-- Name: security_checkautoacceptrule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_checkautoacceptrule (
    id integer NOT NULL,
    sender_profile_id bigint NOT NULL,
    prisoner_profile_id bigint NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: security_checkautoacceptrule_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.security_checkautoacceptrule_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: security_checkautoacceptrule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.security_checkautoacceptrule_id_seq OWNED BY public.security_checkautoacceptrule.id;

--
-- Name: security_checkautoacceptrulestate; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_checkautoacceptrulestate (
    id integer NOT NULL,
    rule_id bigint NOT NULL,
    active boolean DEFAULT true NOT NULL,
    reason text,
    created_by character varying(250),
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: security_checkautoacceptrulestate_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.security_checkautoacceptrulestate_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: security_checkautoacceptrulestate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.security_checkautoacceptrulestate_id_seq OWNED BY public.security_checkautoacceptrulestate.id;

--
-- Name: security_monitoredpartialemailaddress; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_monitoredpartialemailaddress (
    id integer NOT NULL,
    keyword character varying(500) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: security_monitoredpartialemailaddress_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.security_monitoredpartialemailaddress_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: security_monitoredpartialemailaddress_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.security_monitoredpartialemailaddress_id_seq OWNED BY public.security_monitoredpartialemailaddress.id;

--
-- Name: security_prisonerprofile; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_prisonerprofile (
    prisoner_profile_id integer NOT NULL,
    prisoner_number character varying(250),
    prisoner_name character varying(250),
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: security_prisonerprofile_prisoner_profile_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.security_prisonerprofile_prisoner_profile_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: security_prisonerprofile_prisoner_profile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.security_prisonerprofile_prisoner_profile_id_seq OWNED BY public.security_prisonerprofile.prisoner_profile_id;

--
-- Name: security_recipientprofile; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_recipientprofile (
    recipient_profile_id integer NOT NULL,
    sort_code character varying(50),
    account_number character varying(50),
    created timestamp without time zone DEFAULT now() NOT NULL,
    modified timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: security_recipientprofile_recipient_profile_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.security_recipientprofile_recipient_profile_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: security_recipientprofile_recipient_profile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.security_recipientprofile_recipient_profile_id_seq OWNED BY public.security_recipientprofile.recipient_profile_id;

--
-- Name: security_savedsearch; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_savedsearch (
    id integer NOT NULL,
    username character varying(250) NOT NULL,
    description text NOT NULL,
    endpoint character varying(500) NOT NULL,
    filters text,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: security_savedsearch_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.security_savedsearch_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: security_savedsearch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.security_savedsearch_id_seq OWNED BY public.security_savedsearch.id;

--
-- Name: security_senderprofile; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_senderprofile (
    sender_profile_id integer NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL
);

--
-- Name: security_senderprofile_sender_profile_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.security_senderprofile_sender_profile_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: security_senderprofile_sender_profile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.security_senderprofile_sender_profile_id_seq OWNED BY public.security_senderprofile.sender_profile_id;

--
-- Name: sender_profile_credits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sender_profile_credits (
    sender_profile_id integer NOT NULL,
    credit_id integer NOT NULL
);

--
-- Name: sender_profile_monitoring_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sender_profile_monitoring_users (
    sender_profile_id integer NOT NULL,
    user_id character varying(255) NOT NULL
);

--
-- Name: service_downtime; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.service_downtime (
    id integer NOT NULL,
    service character varying(50) NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone,
    message_to_users character varying(255) DEFAULT ''::character varying NOT NULL
);

--
-- Name: service_downtime_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.service_downtime_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: service_downtime_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.service_downtime_id_seq OWNED BY public.service_downtime.id;

--
-- Name: service_notification; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.service_notification (
    id integer NOT NULL,
    public boolean DEFAULT false NOT NULL,
    target character varying(30) NOT NULL,
    level smallint NOT NULL,
    start timestamp without time zone NOT NULL,
    "end" timestamp without time zone,
    headline character varying(200) NOT NULL,
    message text DEFAULT ''::text NOT NULL
);

--
-- Name: service_notification_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.service_notification_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: service_notification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.service_notification_id_seq OWNED BY public.service_notification.id;

--
-- Name: transaction_transaction; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.transaction_transaction (
    transaction_id integer NOT NULL,
    amount bigint DEFAULT 0 NOT NULL,
    sender_sort_code character varying(50),
    sender_account_number character varying(50),
    sender_name character varying(250),
    sender_roll_number character varying(50),
    reference text,
    received_at timestamp without time zone,
    ref_code character varying(50),
    incomplete_sender_info boolean DEFAULT false NOT NULL,
    reference_in_sender_field boolean DEFAULT false NOT NULL,
    credit_id integer,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    category character varying(50) DEFAULT 'CREDIT'::character varying NOT NULL,
    source character varying(50) DEFAULT 'BANK_TRANSFER'::character varying NOT NULL,
    processor_type_code character varying(50)
);

--
-- Name: transaction_transaction_transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.transaction_transaction_transaction_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: transaction_transaction_transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.transaction_transaction_transaction_id_seq OWNED BY public.transaction_transaction.transaction_id;

--
-- Name: user_events; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_events (
    id bigint NOT NULL,
    user_id integer,
    path text,
    data text,
    "timestamp" timestamp without time zone NOT NULL
);

--
-- Name: user_events_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: user_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_events_id_seq OWNED BY public.user_events.id;

--
-- Name: user_flags; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_flags (
    id integer NOT NULL,
    user_id integer NOT NULL,
    flag_name character varying(50) NOT NULL
);

--
-- Name: user_flags_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_flags_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: user_flags_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_flags_id_seq OWNED BY public.user_flags.id;

--
-- Name: balances balance_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balances ALTER COLUMN balance_id SET DEFAULT nextval('public.balances_balance_id_seq'::regclass);

--
-- Name: core_scheduledcommand id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.core_scheduledcommand ALTER COLUMN id SET DEFAULT nextval('public.core_scheduledcommand_id_seq'::regclass);

--
-- Name: credit_comment comment_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_comment ALTER COLUMN comment_id SET DEFAULT nextval('public.credit_comment_comment_id_seq'::regclass);

--
-- Name: credit_credit credit_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_credit ALTER COLUMN credit_id SET DEFAULT nextval('public.credit_credit_credit_id_seq'::regclass);

--
-- Name: credit_log log_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_log ALTER COLUMN log_id SET DEFAULT nextval('public.credit_log_log_id_seq'::regclass);

--
-- Name: credit_processingbatch batch_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_processingbatch ALTER COLUMN batch_id SET DEFAULT nextval('public.credit_processingbatch_batch_id_seq'::regclass);

--
-- Name: disbursement_comment disbursement_comment_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.disbursement_comment ALTER COLUMN disbursement_comment_id SET DEFAULT nextval('public.disbursement_comment_disbursement_comment_id_seq'::regclass);

--
-- Name: disbursement_disbursement disbursement_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.disbursement_disbursement ALTER COLUMN disbursement_id SET DEFAULT nextval('public.disbursement_disbursement_disbursement_id_seq'::regclass);

--
-- Name: disbursement_log disbursement_log_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.disbursement_log ALTER COLUMN disbursement_log_id SET DEFAULT nextval('public.disbursement_log_disbursement_log_id_seq'::regclass);

--
-- Name: file_downloads id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_downloads ALTER COLUMN id SET DEFAULT nextval('public.file_downloads_id_seq'::regclass);

--
-- Name: mtp_auth_accountrequest id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_accountrequest ALTER COLUMN id SET DEFAULT nextval('public.mtp_auth_accountrequest_id_seq'::regclass);

--
-- Name: mtp_auth_failedloginattempt id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_failedloginattempt ALTER COLUMN id SET DEFAULT nextval('public.mtp_auth_failedloginattempt_id_seq'::regclass);

--
-- Name: mtp_auth_jobinformation id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_jobinformation ALTER COLUMN id SET DEFAULT nextval('public.mtp_auth_jobinformation_id_seq'::regclass);

--
-- Name: mtp_auth_login id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_login ALTER COLUMN id SET DEFAULT nextval('public.mtp_auth_login_id_seq'::regclass);

--
-- Name: mtp_auth_role id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_role ALTER COLUMN id SET DEFAULT nextval('public.mtp_auth_role_id_seq'::regclass);

--
-- Name: mtp_users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_users ALTER COLUMN id SET DEFAULT nextval('public.mtp_users_id_seq'::regclass);

--
-- Name: notification_emailnotificationpreferences id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_emailnotificationpreferences ALTER COLUMN id SET DEFAULT nextval('public.notification_emailnotificationpreferences_id_seq'::regclass);

--
-- Name: notification_event id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_event ALTER COLUMN id SET DEFAULT nextval('public.notification_event_id_seq'::regclass);

--
-- Name: password_reset_tokens id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq'::regclass);

--
-- Name: payment_batches payment_batch_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_batches ALTER COLUMN payment_batch_id SET DEFAULT nextval('public.payment_batches_payment_batch_id_seq'::regclass);

--
-- Name: payment_billingaddress billing_address_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_billingaddress ALTER COLUMN billing_address_id SET DEFAULT nextval('public.payment_billingaddress_billing_address_id_seq'::regclass);

--
-- Name: prison_category category_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_category ALTER COLUMN category_id SET DEFAULT nextval('public.prison_category_category_id_seq'::regclass);

--
-- Name: prison_population population_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_population ALTER COLUMN population_id SET DEFAULT nextval('public.prison_population_population_id_seq'::regclass);

--
-- Name: prison_prisonerbalance id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prisonerbalance ALTER COLUMN id SET DEFAULT nextval('public.prison_prisonerbalance_id_seq'::regclass);

--
-- Name: prison_prisonerlocation id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prisonerlocation ALTER COLUMN id SET DEFAULT nextval('public.prison_prisonerlocation_id_seq'::regclass);

--
-- Name: security_check check_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_check ALTER COLUMN check_id SET DEFAULT nextval('public.security_check_check_id_seq'::regclass);

--
-- Name: security_checkautoacceptrule id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_checkautoacceptrule ALTER COLUMN id SET DEFAULT nextval('public.security_checkautoacceptrule_id_seq'::regclass);

--
-- Name: security_checkautoacceptrulestate id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_checkautoacceptrulestate ALTER COLUMN id SET DEFAULT nextval('public.security_checkautoacceptrulestate_id_seq'::regclass);

--
-- Name: security_monitoredpartialemailaddress id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_monitoredpartialemailaddress ALTER COLUMN id SET DEFAULT nextval('public.security_monitoredpartialemailaddress_id_seq'::regclass);

--
-- Name: security_prisonerprofile prisoner_profile_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_prisonerprofile ALTER COLUMN prisoner_profile_id SET DEFAULT nextval('public.security_prisonerprofile_prisoner_profile_id_seq'::regclass);

--
-- Name: security_recipientprofile recipient_profile_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_recipientprofile ALTER COLUMN recipient_profile_id SET DEFAULT nextval('public.security_recipientprofile_recipient_profile_id_seq'::regclass);

--
-- Name: security_savedsearch id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_savedsearch ALTER COLUMN id SET DEFAULT nextval('public.security_savedsearch_id_seq'::regclass);

--
-- Name: security_senderprofile sender_profile_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_senderprofile ALTER COLUMN sender_profile_id SET DEFAULT nextval('public.security_senderprofile_sender_profile_id_seq'::regclass);

--
-- Name: service_downtime id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_downtime ALTER COLUMN id SET DEFAULT nextval('public.service_downtime_id_seq'::regclass);

--
-- Name: service_notification id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_notification ALTER COLUMN id SET DEFAULT nextval('public.service_notification_id_seq'::regclass);

--
-- Name: transaction_transaction transaction_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transaction_transaction ALTER COLUMN transaction_id SET DEFAULT nextval('public.transaction_transaction_transaction_id_seq'::regclass);

--
-- Name: user_events id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_events ALTER COLUMN id SET DEFAULT nextval('public.user_events_id_seq'::regclass);

--
-- Name: user_flags id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_flags ALTER COLUMN id SET DEFAULT nextval('public.user_flags_id_seq'::regclass);

--
-- Name: balances_balance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: core_scheduledcommand_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: credit_comment_comment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: credit_credit_credit_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: credit_log_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: credit_processingbatch_batch_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: disbursement_comment_disbursement_comment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: disbursement_disbursement_disbursement_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: disbursement_log_disbursement_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: file_downloads_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: mtp_auth_accountrequest_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: mtp_auth_failedloginattempt_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: mtp_auth_jobinformation_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: mtp_auth_login_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: mtp_auth_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: mtp_users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: notification_emailnotificationpreferences_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: notification_event_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: payment_batches_payment_batch_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: payment_billingaddress_billing_address_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: prison_category_category_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: prison_population_population_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: prison_prisonerbalance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: prison_prisonerlocation_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: security_check_check_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: security_checkautoacceptrule_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: security_checkautoacceptrulestate_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: security_monitoredpartialemailaddress_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: security_prisonerprofile_prisoner_profile_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: security_recipientprofile_recipient_profile_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: security_savedsearch_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: security_senderprofile_sender_profile_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: service_downtime_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: service_notification_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: transaction_transaction_transaction_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: user_events_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: user_flags_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

--
-- Name: balances balances_date_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balances
    ADD CONSTRAINT balances_date_key UNIQUE (date);

--
--
-- Name: mtp_auth_accountrequest pk_account_requests; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_accountrequest
    ADD CONSTRAINT pk_account_requests PRIMARY KEY (id);

--
-- Name: security_checkautoacceptrulestate pk_auto_accept_rule_states; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_checkautoacceptrulestate
    ADD CONSTRAINT pk_auto_accept_rule_states PRIMARY KEY (id);

--
-- Name: security_checkautoacceptrule pk_auto_accept_rules; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_checkautoacceptrule
    ADD CONSTRAINT pk_auto_accept_rules PRIMARY KEY (id);

--
-- Name: balances pk_balances; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balances
    ADD CONSTRAINT pk_balances PRIMARY KEY (balance_id);

--
-- Name: credit_processingbatch_credits pk_batch_credits; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_processingbatch_credits
    ADD CONSTRAINT pk_batch_credits PRIMARY KEY (batch_id, credit_id);

--
-- Name: credit_processingbatch pk_batches; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_processingbatch
    ADD CONSTRAINT pk_batches PRIMARY KEY (batch_id);

--
-- Name: payment_billingaddress pk_billing_addresses; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_billingaddress
    ADD CONSTRAINT pk_billing_addresses PRIMARY KEY (billing_address_id);

--
-- Name: credit_comment pk_comments; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_comment
    ADD CONSTRAINT pk_comments PRIMARY KEY (comment_id);

--
-- Name: credit_credit pk_credits; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_credit
    ADD CONSTRAINT pk_credits PRIMARY KEY (credit_id);

--
-- Name: disbursement_comment pk_disbursement_comments; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.disbursement_comment
    ADD CONSTRAINT pk_disbursement_comments PRIMARY KEY (disbursement_comment_id);

--
-- Name: disbursement_log pk_disbursement_logs; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.disbursement_log
    ADD CONSTRAINT pk_disbursement_logs PRIMARY KEY (disbursement_log_id);

--
-- Name: disbursement_disbursement pk_disbursements; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.disbursement_disbursement
    ADD CONSTRAINT pk_disbursements PRIMARY KEY (disbursement_id);

--
-- Name: notification_emailnotificationpreferences pk_email_notification_preferences; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_emailnotificationpreferences
    ADD CONSTRAINT pk_email_notification_preferences PRIMARY KEY (id);

--
-- Name: mtp_auth_failedloginattempt pk_failed_login_attempts; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_failedloginattempt
    ADD CONSTRAINT pk_failed_login_attempts PRIMARY KEY (id);

--
-- Name: file_downloads pk_file_downloads; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_downloads
    ADD CONSTRAINT pk_file_downloads PRIMARY KEY (id);

--
-- Name: mtp_auth_jobinformation pk_job_information; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_jobinformation
    ADD CONSTRAINT pk_job_information PRIMARY KEY (id);

--
-- Name: credit_log pk_logs; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_log
    ADD CONSTRAINT pk_logs PRIMARY KEY (log_id);

--
-- Name: security_monitoredpartialemailaddress pk_monitored_partial_email_addresses; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_monitoredpartialemailaddress
    ADD CONSTRAINT pk_monitored_partial_email_addresses PRIMARY KEY (id);

--
-- Name: mtp_auth_login pk_mtp_logins; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_login
    ADD CONSTRAINT pk_mtp_logins PRIMARY KEY (id);

--
-- Name: mtp_auth_role pk_mtp_roles; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_role
    ADD CONSTRAINT pk_mtp_roles PRIMARY KEY (id);

--
-- Name: mtp_auth_prisonusermapping_prisons pk_mtp_user_prisons; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_prisonusermapping_prisons
    ADD CONSTRAINT pk_mtp_user_prisons PRIMARY KEY (user_id, prison_nomis_id);

--
-- Name: mtp_users pk_mtp_users; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_users
    ADD CONSTRAINT pk_mtp_users PRIMARY KEY (id);

--
-- Name: notification_event pk_notification_events; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_event
    ADD CONSTRAINT pk_notification_events PRIMARY KEY (id);

--
-- Name: password_reset_tokens pk_password_reset_tokens; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id);

--
-- Name: payment_batch_credits pk_payment_batch_credits; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_batch_credits
    ADD CONSTRAINT pk_payment_batch_credits PRIMARY KEY (payment_batch_id, credit_id);

--
-- Name: payment_batches pk_payment_batches; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_batches
    ADD CONSTRAINT pk_payment_batches PRIMARY KEY (payment_batch_id);

--
-- Name: payment_payment pk_payments; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_payment
    ADD CONSTRAINT pk_payments PRIMARY KEY (uuid);

--
-- Name: prison_category pk_prison_categories; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_category
    ADD CONSTRAINT pk_prison_categories PRIMARY KEY (category_id);

--
-- Name: prison_population pk_prison_populations; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_population
    ADD CONSTRAINT pk_prison_populations PRIMARY KEY (population_id);

--
-- Name: prison_prison_categories pk_prison_prison_categories; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prison_categories
    ADD CONSTRAINT pk_prison_prison_categories PRIMARY KEY (prison_nomis_id, category_id);

--
-- Name: prison_prison_populations pk_prison_prison_populations; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prison_populations
    ADD CONSTRAINT pk_prison_prison_populations PRIMARY KEY (prison_nomis_id, population_id);

--
-- Name: prison_prisonerbalance pk_prisoner_balances; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prisonerbalance
    ADD CONSTRAINT pk_prisoner_balances PRIMARY KEY (id);

--
-- Name: prison_prisonercreditnoticeemail pk_prisoner_credit_notice_emails; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prisonercreditnoticeemail
    ADD CONSTRAINT pk_prisoner_credit_notice_emails PRIMARY KEY (prison_id);

--
-- Name: prison_prisonerlocation pk_prisoner_locations; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prisonerlocation
    ADD CONSTRAINT pk_prisoner_locations PRIMARY KEY (id);

--
-- Name: prisoner_profile_credits pk_prisoner_profile_credits; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prisoner_profile_credits
    ADD CONSTRAINT pk_prisoner_profile_credits PRIMARY KEY (prisoner_profile_id, credit_id);

--
-- Name: prisoner_profile_monitoring_users pk_prisoner_profile_monitoring_users; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prisoner_profile_monitoring_users
    ADD CONSTRAINT pk_prisoner_profile_monitoring_users PRIMARY KEY (prisoner_profile_id, user_id);

--
-- Name: security_prisonerprofile pk_prisoner_profiles; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_prisonerprofile
    ADD CONSTRAINT pk_prisoner_profiles PRIMARY KEY (prisoner_profile_id);

--
-- Name: prison_prison pk_prisons; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prison
    ADD CONSTRAINT pk_prisons PRIMARY KEY (nomis_id);

--
-- Name: credit_privateestatebatch_credits pk_private_estate_batch_credits; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_privateestatebatch_credits
    ADD CONSTRAINT pk_private_estate_batch_credits PRIMARY KEY (ref, credit_id);

--
-- Name: credit_privateestatebatch pk_private_estate_batches; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_privateestatebatch
    ADD CONSTRAINT pk_private_estate_batches PRIMARY KEY (ref);

--
-- Name: security_savedsearch pk_saved_searches; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_savedsearch
    ADD CONSTRAINT pk_saved_searches PRIMARY KEY (id);

--
-- Name: core_scheduledcommand pk_scheduled_commands; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.core_scheduledcommand
    ADD CONSTRAINT pk_scheduled_commands PRIMARY KEY (id);

--
-- Name: security_check pk_security_checks; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_check
    ADD CONSTRAINT pk_security_checks PRIMARY KEY (check_id);

--
-- Name: sender_profile_credits pk_sender_profile_credits; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sender_profile_credits
    ADD CONSTRAINT pk_sender_profile_credits PRIMARY KEY (sender_profile_id, credit_id);

--
-- Name: sender_profile_monitoring_users pk_sender_profile_monitoring_users; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sender_profile_monitoring_users
    ADD CONSTRAINT pk_sender_profile_monitoring_users PRIMARY KEY (sender_profile_id, user_id);

--
-- Name: security_senderprofile pk_sender_profiles; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_senderprofile
    ADD CONSTRAINT pk_sender_profiles PRIMARY KEY (sender_profile_id);

--
-- Name: service_downtime pk_service_downtime; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_downtime
    ADD CONSTRAINT pk_service_downtime PRIMARY KEY (id);

--
-- Name: service_notification pk_service_notification; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_notification
    ADD CONSTRAINT pk_service_notification PRIMARY KEY (id);

--
-- Name: transaction_transaction pk_transactions; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transaction_transaction
    ADD CONSTRAINT pk_transactions PRIMARY KEY (transaction_id);

--
-- Name: user_events pk_user_events; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_events
    ADD CONSTRAINT pk_user_events PRIMARY KEY (id);

--
-- Name: user_flags pk_user_flags; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_flags
    ADD CONSTRAINT pk_user_flags PRIMARY KEY (id);

--
-- Name: security_recipientprofile security_recipientprofile_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_recipientprofile
    ADD CONSTRAINT security_recipientprofile_pkey PRIMARY KEY (recipient_profile_id);

--
-- Name: security_checkautoacceptrule uq_auto_accept_rules_pair; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_checkautoacceptrule
    ADD CONSTRAINT uq_auto_accept_rules_pair UNIQUE (sender_profile_id, prisoner_profile_id);

--
-- Name: notification_emailnotificationpreferences uq_email_notification_preferences_username; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_emailnotificationpreferences
    ADD CONSTRAINT uq_email_notification_preferences_username UNIQUE (username);

--
-- Name: file_downloads uq_file_downloads_label_date; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_downloads
    ADD CONSTRAINT uq_file_downloads_label_date UNIQUE (label, date);

--
-- Name: security_monitoredpartialemailaddress uq_monitored_partial_email_keyword; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_monitoredpartialemailaddress
    ADD CONSTRAINT uq_monitored_partial_email_keyword UNIQUE (keyword);

--
-- Name: mtp_auth_role uq_mtp_roles_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_role
    ADD CONSTRAINT uq_mtp_roles_name UNIQUE (name);

--
-- Name: mtp_users uq_mtp_users_username; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_users
    ADD CONSTRAINT uq_mtp_users_username UNIQUE (username);

--
-- Name: password_reset_tokens uq_password_reset_tokens_token; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT uq_password_reset_tokens_token UNIQUE (token);

--
-- Name: payment_batches uq_payment_batches_ref_code; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_batches
    ADD CONSTRAINT uq_payment_batches_ref_code UNIQUE (ref_code);

--
-- Name: payment_payment uq_payments_credit_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_payment
    ADD CONSTRAINT uq_payments_credit_id UNIQUE (credit_id);

--
-- Name: prison_category uq_prison_categories_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_category
    ADD CONSTRAINT uq_prison_categories_name UNIQUE (name);

--
-- Name: prison_population uq_prison_populations_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_population
    ADD CONSTRAINT uq_prison_populations_name UNIQUE (name);

--
-- Name: security_check uq_security_checks_credit; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_check
    ADD CONSTRAINT uq_security_checks_credit UNIQUE (credit_id);

--
-- Name: transaction_transaction uq_transactions_credit_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transaction_transaction
    ADD CONSTRAINT uq_transactions_credit_id UNIQUE (credit_id);

--
-- Name: user_flags uq_user_flags_user_flag; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_flags
    ADD CONSTRAINT uq_user_flags_user_flag UNIQUE (user_id, flag_name);

--
--
-- Name: idx_account_requests_status_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_account_requests_status_created ON public.mtp_auth_accountrequest USING btree (status, created DESC);

--
-- Name: idx_batch_credits_batch_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_batch_credits_batch_id ON public.credit_processingbatch_credits USING btree (batch_id);

--
-- Name: idx_batch_credits_credit_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_batch_credits_credit_id ON public.credit_processingbatch_credits USING btree (credit_id);

--
-- Name: idx_batches_owner; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_batches_owner ON public.credit_processingbatch USING btree (owner);

--
-- Name: idx_comments_credit_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_comments_credit_id ON public.credit_comment USING btree (credit_id);

--
-- Name: idx_credits_amount; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_credits_amount ON public.credit_credit USING btree (amount);

--
-- Name: idx_credits_owner; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_credits_owner ON public.credit_credit USING btree (owner);

--
-- Name: idx_credits_prisoner_number; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_credits_prisoner_number ON public.credit_credit USING btree (prisoner_number);

--
-- Name: idx_credits_received_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_credits_received_at ON public.credit_credit USING btree (received_at);

--
-- Name: idx_credits_resolution; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_credits_resolution ON public.credit_credit USING btree (resolution);

--
-- Name: idx_disbursements_prison; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_disbursements_prison ON public.disbursement_disbursement USING btree (prison);

--
-- Name: idx_disbursements_prisoner_number; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_disbursements_prisoner_number ON public.disbursement_disbursement USING btree (prisoner_number);

--
-- Name: idx_disbursements_resolution; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_disbursements_resolution ON public.disbursement_disbursement USING btree (resolution);

--
-- Name: idx_failed_login_attempts_user_app; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_failed_login_attempts_user_app ON public.mtp_auth_failedloginattempt USING btree (user_id, application, attempted_at DESC);

--
-- Name: idx_file_downloads_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_file_downloads_date ON public.file_downloads USING btree (date);

--
-- Name: idx_file_downloads_label; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_file_downloads_label ON public.file_downloads USING btree (label);

--
-- Name: idx_job_information_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_job_information_user_id ON public.mtp_auth_jobinformation USING btree (user_id);

--
-- Name: idx_logs_action; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logs_action ON public.credit_log USING btree (action);

--
-- Name: idx_logs_credit_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_logs_credit_id ON public.credit_log USING btree (credit_id);

--
-- Name: idx_notification_events_rule; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notification_events_rule ON public.notification_event USING btree (rule);

--
-- Name: idx_notification_events_triggered_at_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notification_events_triggered_at_id ON public.notification_event USING btree (triggered_at DESC, id);

--
-- Name: idx_payment_batch_credits_batch_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payment_batch_credits_batch_id ON public.payment_batch_credits USING btree (payment_batch_id);

--
-- Name: idx_payment_batch_credits_credit_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payment_batch_credits_credit_id ON public.payment_batch_credits USING btree (credit_id);

--
-- Name: idx_payment_batches_settlement_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payment_batches_settlement_date ON public.payment_batches USING btree (settlement_date);

--
-- Name: idx_peb_credits_ref; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_peb_credits_ref ON public.credit_privateestatebatch_credits USING btree (ref);

--
-- Name: idx_prisoner_locations_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prisoner_locations_active ON public.prison_prisonerlocation USING btree (active);

--
-- Name: idx_prisoner_locations_prisoner_number; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prisoner_locations_prisoner_number ON public.prison_prisonerlocation USING btree (prisoner_number);

--
-- Name: idx_private_estate_batches_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_private_estate_batches_date ON public.credit_privateestatebatch USING btree (date);

--
-- Name: idx_private_estate_batches_prison; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_private_estate_batches_prison ON public.credit_privateestatebatch USING btree (prison);

--
-- Name: idx_scheduled_commands_next_execution; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scheduled_commands_next_execution ON public.core_scheduledcommand USING btree (next_execution);

--
-- Name: idx_security_checks_credit_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_security_checks_credit_id ON public.security_check USING btree (credit_id);

--
-- Name: idx_security_checks_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_security_checks_status ON public.security_check USING btree (status);

--
-- Name: idx_service_downtime_service_start; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_service_downtime_service_start ON public.service_downtime USING btree (service, start_time);

--
-- Name: idx_service_notification_start_end; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_service_notification_start_end ON public.service_notification USING btree (start, "end");

--
-- Name: idx_user_events_timestamp_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_events_timestamp_id ON public.user_events USING btree ("timestamp" DESC, id DESC);

--
-- Name: security_checkautoacceptrule fk_aar_prisoner_profile; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_checkautoacceptrule
    ADD CONSTRAINT fk_aar_prisoner_profile FOREIGN KEY (prisoner_profile_id) REFERENCES public.security_prisonerprofile(prisoner_profile_id) ON DELETE CASCADE;

--
-- Name: security_checkautoacceptrule fk_aar_sender_profile; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_checkautoacceptrule
    ADD CONSTRAINT fk_aar_sender_profile FOREIGN KEY (sender_profile_id) REFERENCES public.security_senderprofile(sender_profile_id) ON DELETE CASCADE;

--
-- Name: security_checkautoacceptrulestate fk_aars_rule; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_checkautoacceptrulestate
    ADD CONSTRAINT fk_aars_rule FOREIGN KEY (rule_id) REFERENCES public.security_checkautoacceptrule(id) ON DELETE CASCADE;

--
-- Name: credit_processingbatch_credits fk_batch_credits_batch; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_processingbatch_credits
    ADD CONSTRAINT fk_batch_credits_batch FOREIGN KEY (batch_id) REFERENCES public.credit_processingbatch(batch_id) ON DELETE CASCADE;

--
-- Name: credit_processingbatch_credits fk_batch_credits_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_processingbatch_credits
    ADD CONSTRAINT fk_batch_credits_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: credit_comment fk_comments_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_comment
    ADD CONSTRAINT fk_comments_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: credit_credit fk_credits_prison; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_credit
    ADD CONSTRAINT fk_credits_prison FOREIGN KEY (prison) REFERENCES public.prison_prison(nomis_id);

--
-- Name: disbursement_comment fk_disbursement_comments_disbursement; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.disbursement_comment
    ADD CONSTRAINT fk_disbursement_comments_disbursement FOREIGN KEY (disbursement_id) REFERENCES public.disbursement_disbursement(disbursement_id);

--
-- Name: disbursement_log fk_disbursement_logs_disbursement; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.disbursement_log
    ADD CONSTRAINT fk_disbursement_logs_disbursement FOREIGN KEY (disbursement_id) REFERENCES public.disbursement_disbursement(disbursement_id);

--
-- Name: credit_log fk_logs_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_log
    ADD CONSTRAINT fk_logs_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: notification_event fk_notification_events_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_event
    ADD CONSTRAINT fk_notification_events_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id) ON DELETE CASCADE;

--
-- Name: notification_event fk_notification_events_disbursement; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_event
    ADD CONSTRAINT fk_notification_events_disbursement FOREIGN KEY (disbursement_id) REFERENCES public.disbursement_disbursement(disbursement_id) ON DELETE CASCADE;

--
-- Name: notification_event fk_notification_events_prisoner_profile; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_event
    ADD CONSTRAINT fk_notification_events_prisoner_profile FOREIGN KEY (prisoner_profile_id) REFERENCES public.security_prisonerprofile(prisoner_profile_id) ON DELETE CASCADE;

--
-- Name: notification_event fk_notification_events_sender_profile; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_event
    ADD CONSTRAINT fk_notification_events_sender_profile FOREIGN KEY (sender_profile_id) REFERENCES public.security_senderprofile(sender_profile_id) ON DELETE CASCADE;

--
-- Name: payment_batch_credits fk_payment_batch_credits_batch; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_batch_credits
    ADD CONSTRAINT fk_payment_batch_credits_batch FOREIGN KEY (payment_batch_id) REFERENCES public.payment_batches(payment_batch_id) ON DELETE CASCADE;

--
-- Name: payment_batch_credits fk_payment_batch_credits_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_batch_credits
    ADD CONSTRAINT fk_payment_batch_credits_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: payment_payment fk_payments_billing_address; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_payment
    ADD CONSTRAINT fk_payments_billing_address FOREIGN KEY (billing_address_id) REFERENCES public.payment_billingaddress(billing_address_id);

--
-- Name: payment_payment fk_payments_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payment_payment
    ADD CONSTRAINT fk_payments_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: credit_privateestatebatch_credits fk_peb_credits_batch; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_privateestatebatch_credits
    ADD CONSTRAINT fk_peb_credits_batch FOREIGN KEY (ref) REFERENCES public.credit_privateestatebatch(ref) ON DELETE CASCADE;

--
-- Name: credit_privateestatebatch_credits fk_peb_credits_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_privateestatebatch_credits
    ADD CONSTRAINT fk_peb_credits_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: prison_prison_categories fk_ppc_category; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prison_categories
    ADD CONSTRAINT fk_ppc_category FOREIGN KEY (category_id) REFERENCES public.prison_category(category_id);

--
-- Name: prisoner_profile_credits fk_ppc_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prisoner_profile_credits
    ADD CONSTRAINT fk_ppc_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: prison_prison_categories fk_ppc_prison; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prison_categories
    ADD CONSTRAINT fk_ppc_prison FOREIGN KEY (prison_nomis_id) REFERENCES public.prison_prison(nomis_id);

--
-- Name: prisoner_profile_credits fk_ppc_prisoner_profile; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prisoner_profile_credits
    ADD CONSTRAINT fk_ppc_prisoner_profile FOREIGN KEY (prisoner_profile_id) REFERENCES public.security_prisonerprofile(prisoner_profile_id);

--
-- Name: prisoner_profile_monitoring_users fk_ppmu_prisoner_profile; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prisoner_profile_monitoring_users
    ADD CONSTRAINT fk_ppmu_prisoner_profile FOREIGN KEY (prisoner_profile_id) REFERENCES public.security_prisonerprofile(prisoner_profile_id);

--
-- Name: prison_prison_populations fk_ppp_population; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prison_populations
    ADD CONSTRAINT fk_ppp_population FOREIGN KEY (population_id) REFERENCES public.prison_population(population_id);

--
-- Name: prison_prison_populations fk_ppp_prison; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prison_populations
    ADD CONSTRAINT fk_ppp_prison FOREIGN KEY (prison_nomis_id) REFERENCES public.prison_prison(nomis_id);

--
-- Name: prison_prisonerbalance fk_prisoner_balances_prison; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prisonerbalance
    ADD CONSTRAINT fk_prisoner_balances_prison FOREIGN KEY (prison_id) REFERENCES public.prison_prison(nomis_id) ON DELETE CASCADE;

--
-- Name: prison_prisonercreditnoticeemail fk_prisoner_credit_notice_emails_prison; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prisonercreditnoticeemail
    ADD CONSTRAINT fk_prisoner_credit_notice_emails_prison FOREIGN KEY (prison_id) REFERENCES public.prison_prison(nomis_id) ON DELETE CASCADE;

--
-- Name: prison_prisonerlocation fk_prisoner_locations_prison; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prison_prisonerlocation
    ADD CONSTRAINT fk_prisoner_locations_prison FOREIGN KEY (prison_id) REFERENCES public.prison_prison(nomis_id) ON DELETE CASCADE;

--
-- Name: credit_privateestatebatch fk_private_estate_batches_prison; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credit_privateestatebatch
    ADD CONSTRAINT fk_private_estate_batches_prison FOREIGN KEY (prison) REFERENCES public.prison_prison(nomis_id);

--
-- Name: security_check fk_security_checks_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_check
    ADD CONSTRAINT fk_security_checks_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: sender_profile_credits fk_spc_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sender_profile_credits
    ADD CONSTRAINT fk_spc_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: sender_profile_credits fk_spc_sender_profile; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sender_profile_credits
    ADD CONSTRAINT fk_spc_sender_profile FOREIGN KEY (sender_profile_id) REFERENCES public.security_senderprofile(sender_profile_id);

--
-- Name: sender_profile_monitoring_users fk_spmu_sender_profile; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sender_profile_monitoring_users
    ADD CONSTRAINT fk_spmu_sender_profile FOREIGN KEY (sender_profile_id) REFERENCES public.security_senderprofile(sender_profile_id);

--
-- Name: transaction_transaction fk_transactions_credit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transaction_transaction
    ADD CONSTRAINT fk_transactions_credit FOREIGN KEY (credit_id) REFERENCES public.credit_credit(credit_id);

--
-- Name: mtp_auth_accountrequest mtp_auth_accountrequest_prison_nomis_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_accountrequest
    ADD CONSTRAINT mtp_auth_accountrequest_prison_nomis_id_fkey FOREIGN KEY (prison_nomis_id) REFERENCES public.prison_prison(nomis_id);

--
-- Name: mtp_auth_accountrequest mtp_auth_accountrequest_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_accountrequest
    ADD CONSTRAINT mtp_auth_accountrequest_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.mtp_auth_role(id);

--
-- Name: mtp_auth_failedloginattempt mtp_auth_failedloginattempt_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_failedloginattempt
    ADD CONSTRAINT mtp_auth_failedloginattempt_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.mtp_users(id) ON DELETE CASCADE;

--
-- Name: mtp_auth_jobinformation mtp_auth_jobinformation_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_jobinformation
    ADD CONSTRAINT mtp_auth_jobinformation_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.mtp_users(id);

--
-- Name: mtp_auth_login mtp_auth_login_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_login
    ADD CONSTRAINT mtp_auth_login_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.mtp_users(id) ON DELETE CASCADE;

--
-- Name: mtp_auth_prisonusermapping_prisons mtp_auth_prisonusermapping_prisons_prison_nomis_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_prisonusermapping_prisons
    ADD CONSTRAINT mtp_auth_prisonusermapping_prisons_prison_nomis_id_fkey FOREIGN KEY (prison_nomis_id) REFERENCES public.prison_prison(nomis_id) ON DELETE CASCADE;

--
-- Name: mtp_auth_prisonusermapping_prisons mtp_auth_prisonusermapping_prisons_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_auth_prisonusermapping_prisons
    ADD CONSTRAINT mtp_auth_prisonusermapping_prisons_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.mtp_users(id) ON DELETE CASCADE;

--
-- Name: mtp_users mtp_users_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mtp_users
    ADD CONSTRAINT mtp_users_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.mtp_auth_role(id);

--
-- Name: password_reset_tokens password_reset_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.mtp_users(id) ON DELETE CASCADE;

--
-- Name: recipient_profile_monitoring_users recipient_profile_monitoring_users_recipient_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.recipient_profile_monitoring_users
    ADD CONSTRAINT recipient_profile_monitoring_users_recipient_profile_id_fkey FOREIGN KEY (recipient_profile_id) REFERENCES public.security_recipientprofile(recipient_profile_id) ON DELETE CASCADE;

--
-- Name: user_events user_events_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_events
    ADD CONSTRAINT user_events_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.mtp_users(id);

--
-- Name: user_flags user_flags_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_flags
    ADD CONSTRAINT user_flags_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.mtp_users(id) ON DELETE CASCADE;

--
-- PostgreSQL database dump complete
--


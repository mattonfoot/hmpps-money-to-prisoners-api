CREATE TABLE IF NOT EXISTS security_recipientprofile (
    recipient_profile_id SERIAL PRIMARY KEY,
    sort_code            VARCHAR(50),
    account_number       VARCHAR(50),
    created              TIMESTAMP NOT NULL DEFAULT NOW(),
    modified             TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recipient_profile_monitoring_users (
    recipient_profile_id BIGINT NOT NULL REFERENCES security_recipientprofile(recipient_profile_id) ON DELETE CASCADE,
    user_id              VARCHAR(255) NOT NULL
);

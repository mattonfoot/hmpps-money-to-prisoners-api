-- Django authentication tables required for shared auth between Python and Kotlin APIs

-- Django content types (required for auth_permission FK)
CREATE TABLE IF NOT EXISTS django_content_type (
    id SERIAL PRIMARY KEY,
    app_label VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    UNIQUE (app_label, model)
);

-- Django auth_user (the canonical user table)
CREATE TABLE IF NOT EXISTS auth_user (
    id SERIAL PRIMARY KEY,
    password VARCHAR(128) NOT NULL,
    last_login TIMESTAMP WITH TIME ZONE,
    is_superuser BOOLEAN NOT NULL DEFAULT FALSE,
    username VARCHAR(150) NOT NULL UNIQUE,
    first_name VARCHAR(150) NOT NULL DEFAULT '',
    last_name VARCHAR(150) NOT NULL DEFAULT '',
    email VARCHAR(254) NOT NULL DEFAULT '',
    is_staff BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    date_joined TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS auth_user_username_idx ON auth_user (username);

-- Django auth_group
CREATE TABLE IF NOT EXISTS auth_group (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
);

-- Django auth_permission
CREATE TABLE IF NOT EXISTS auth_permission (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content_type_id INTEGER NOT NULL REFERENCES django_content_type(id),
    codename VARCHAR(100) NOT NULL,
    UNIQUE (content_type_id, codename)
);

-- auth_user_groups (M2M: user → group)
CREATE TABLE IF NOT EXISTS auth_user_groups (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
    group_id INTEGER NOT NULL REFERENCES auth_group(id) ON DELETE CASCADE,
    UNIQUE (user_id, group_id)
);

CREATE INDEX IF NOT EXISTS auth_user_groups_user_id_idx ON auth_user_groups (user_id);
CREATE INDEX IF NOT EXISTS auth_user_groups_group_id_idx ON auth_user_groups (group_id);

-- auth_group_permissions (M2M: group → permission)
CREATE TABLE IF NOT EXISTS auth_group_permissions (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES auth_group(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES auth_permission(id) ON DELETE CASCADE,
    UNIQUE (group_id, permission_id)
);

-- auth_user_user_permissions (M2M: user → permission, direct)
CREATE TABLE IF NOT EXISTS auth_user_user_permissions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES auth_permission(id) ON DELETE CASCADE,
    UNIQUE (user_id, permission_id)
);

-- OAuth2 Provider: Applications
CREATE TABLE IF NOT EXISTS oauth2_provider_application (
    id SERIAL PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_secret VARCHAR(255) NOT NULL DEFAULT '',
    client_type VARCHAR(32) NOT NULL DEFAULT 'confidential',
    authorization_grant_type VARCHAR(32) NOT NULL DEFAULT 'password',
    name VARCHAR(255) NOT NULL DEFAULT '',
    user_id INTEGER REFERENCES auth_user(id) ON DELETE CASCADE,
    redirect_uris TEXT NOT NULL DEFAULT '',
    skip_authorization BOOLEAN NOT NULL DEFAULT FALSE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- OAuth2 Provider: Access Tokens
CREATE TABLE IF NOT EXISTS oauth2_provider_accesstoken (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires TIMESTAMP WITH TIME ZONE NOT NULL,
    scope TEXT NOT NULL DEFAULT '',
    application_id INTEGER REFERENCES oauth2_provider_application(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES auth_user(id) ON DELETE CASCADE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    source_refresh_token_id BIGINT
);

CREATE INDEX IF NOT EXISTS oauth2_provider_accesstoken_token_idx ON oauth2_provider_accesstoken (token);
CREATE INDEX IF NOT EXISTS oauth2_provider_accesstoken_user_id_idx ON oauth2_provider_accesstoken (user_id);

-- MTP auth: Application-User mapping (which users can authenticate with which apps)
CREATE TABLE IF NOT EXISTS mtp_auth_applicationusermapping (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
    application_id INTEGER NOT NULL REFERENCES oauth2_provider_application(id) ON DELETE CASCADE,
    UNIQUE (user_id, application_id)
);

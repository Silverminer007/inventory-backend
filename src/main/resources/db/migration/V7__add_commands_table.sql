CREATE TABLE commands (
    id              BIGSERIAL    PRIMARY KEY,
    command_id      UUID         NOT NULL UNIQUE,
    command_type    VARCHAR(50)  NOT NULL,
    payload_version INTEGER      NOT NULL DEFAULT 1,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       BIGINT,
    payload         JSONB        NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    client_id       VARCHAR(255),
    client_sequence BIGINT,
    issued_at       TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    applied_at      TIMESTAMP,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message   TEXT
);

CREATE INDEX idx_commands_user_id    ON commands(user_id);
CREATE INDEX idx_commands_entity     ON commands(entity_type, entity_id);
CREATE INDEX idx_commands_created_at ON commands(created_at);
CREATE INDEX idx_commands_client     ON commands(client_id, client_sequence);

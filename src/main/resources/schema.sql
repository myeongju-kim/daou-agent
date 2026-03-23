CREATE TABLE IF NOT EXISTS agent_sessions (
    id VARCHAR(100) PRIMARY KEY,
    agent_key VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    selected_model VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS agent_session_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    message_order INT NOT NULL,
    role VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_agent_session_messages_session
        FOREIGN KEY (session_id) REFERENCES agent_sessions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS agent_approval_requests (
    id VARCHAR(100) PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    tool_arguments TEXT NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    requested_user_message TEXT NOT NULL,
    summary_snapshot TEXT NOT NULL,
    selected_model_snapshot VARCHAR(255) NOT NULL,
    created_correlation_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    decided_at TIMESTAMP(6) NULL
);

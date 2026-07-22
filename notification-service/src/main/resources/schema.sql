CREATE TABLE IF NOT EXISTS processed_event (
                                               idempotency_key VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
    );
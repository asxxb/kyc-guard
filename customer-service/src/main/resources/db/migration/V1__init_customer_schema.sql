CREATE TABLE customer (
    id UUID PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    incorporation_date DATE,
    country VARCHAR(2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    risk_band VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_status ON customer(status);
CREATE INDEX idx_customer_risk_band ON customer(risk_band);
CREATE INDEX idx_customer_country ON customer(country);

CREATE TABLE customer_alias (
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    alias VARCHAR(255) NOT NULL,
    PRIMARY KEY (customer_id, alias)
);

CREATE TABLE customer_address (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    street VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(30),
    country VARCHAR(2) NOT NULL
);

CREATE INDEX idx_customer_address_customer_id ON customer_address(customer_id);

CREATE TABLE customer_identifier (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    identifier_value VARCHAR(100) NOT NULL
);

CREATE INDEX idx_customer_identifier_customer_id ON customer_identifier(customer_id);
CREATE INDEX idx_customer_identifier_lookup ON customer_identifier(type, identifier_value);

CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    response_status INT NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(128),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status_created ON outbox(status, created_at);

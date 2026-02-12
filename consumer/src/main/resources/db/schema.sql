CREATE TABLE IF NOT EXISTS orders (
    id            BIGSERIAL       PRIMARY KEY,
    order_id      VARCHAR(255),
    product_name  VARCHAR(255),
    quantity      INTEGER         NOT NULL,
    created_at    TIMESTAMP,
    consumed_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customer (
  customer_id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL,
  mobile_number VARCHAR(20) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(20) NOT NULL,
  updated_at TIMESTAMP DEFAULT NULL,
  updated_by VARCHAR(20) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS account (
  account_id BIGSERIAL PRIMARY KEY,
  account_number VARCHAR(20) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  account_type VARCHAR(100) NOT NULL,
  branch_address VARCHAR(200) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(20) NOT NULL,
  updated_at TIMESTAMP DEFAULT NULL,
  updated_by VARCHAR(20) DEFAULT NULL,
  CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE CASCADE
);
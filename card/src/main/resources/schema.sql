CREATE TABLE IF NOT EXISTS card (
  card_id BIGSERIAL PRIMARY KEY,
  card_number VARCHAR(16) UNIQUE NOT NULL,
  mobile_number VARCHAR(15) NOT NULL UNIQUE,
  card_type VARCHAR(100) NOT NULL,
  total_limit INT NOT NULL,
  amount_used INT NOT NULL,
  available_amount INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(20) NOT NULL,
  updated_at TIMESTAMP,
  updated_by VARCHAR(20)
);

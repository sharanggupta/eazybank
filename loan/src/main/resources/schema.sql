CREATE TABLE IF NOT EXISTS loan (
  loan_number VARCHAR(12) PRIMARY KEY,
  mobile_number VARCHAR(15) NOT NULL,
  loan_type VARCHAR(100) NOT NULL,
  total_loan INT NOT NULL,
  amount_paid INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(20) NOT NULL,
  updated_at TIMESTAMP,
  updated_by VARCHAR(20)
);
CREATE TABLE accounts (
    id BINARY(16) NOT NULL,
    account_number VARCHAR(34) NOT NULL,
    account_holder_name VARCHAR(120) NOT NULL,
    account_open_date DATE NOT NULL,
    pin_code VARCHAR(20) NOT NULL,
    ifsc VARCHAR(11) NOT NULL,
    branch VARCHAR(120) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
) ENGINE = InnoDB;

CREATE TABLE ledger_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BINARY(16) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    reference_id BINARY(16) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT chk_ledger_entries_amount_positive CHECK (amount > 0),
    INDEX idx_ledger_account_time (account_id, occurred_at DESC, id DESC),
    INDEX idx_ledger_reference (reference_id)
) ENGINE = InnoDB;

ALTER TABLE ledger_entries
    DROP INDEX idx_ledger_account_time,
    CHANGE COLUMN occurred_at created_at TIMESTAMP(6) NOT NULL,
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD INDEX idx_ledger_account_time (account_number, created_at DESC, id DESC);

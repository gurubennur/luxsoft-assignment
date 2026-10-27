ALTER TABLE accounts
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN modified_by VARCHAR(120) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by VARCHAR(120) NOT NULL DEFAULT 'system';

ALTER TABLE ledger_entries
    DROP FOREIGN KEY fk_ledger_entries_account,
    DROP INDEX idx_ledger_account_time,
    ADD COLUMN account_number VARCHAR(34) NULL AFTER account_id;

UPDATE ledger_entries AS entry
JOIN accounts AS account ON account.id = entry.account_id
SET entry.account_number = account.account_number;

ALTER TABLE accounts
    DROP PRIMARY KEY,
    DROP INDEX uk_accounts_account_number,
    ADD PRIMARY KEY (account_number),
    DROP COLUMN id;

ALTER TABLE ledger_entries
    MODIFY COLUMN account_number VARCHAR(34) NOT NULL,
    ADD CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_number) REFERENCES accounts (account_number),
    ADD INDEX idx_ledger_account_time (account_number, occurred_at DESC, id DESC),
    DROP COLUMN account_id;

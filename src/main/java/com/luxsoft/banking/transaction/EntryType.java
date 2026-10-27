package com.luxsoft.banking.transaction;

/** Identifies the account-side effect recorded by a ledger entry. */
public enum EntryType {
    OPENING_BALANCE, DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
}

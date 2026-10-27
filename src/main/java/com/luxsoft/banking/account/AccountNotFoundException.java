package com.luxsoft.banking.account;

public class AccountNotFoundException extends RuntimeException {
    /** Creates a domain error for an account number that does not exist. */
    public AccountNotFoundException(String accountNumber) { super("Account not found: " + accountNumber); }
}

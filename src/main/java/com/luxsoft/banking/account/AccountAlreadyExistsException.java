package com.luxsoft.banking.account;

public class AccountAlreadyExistsException extends RuntimeException {
    /** Creates a domain error for an account number that is already in use. */
    public AccountAlreadyExistsException(String accountNumber) {
        super("An account already exists with account number: " + accountNumber);
    }
}

package com.luxsoft.banking.account;

import com.luxsoft.banking.api.ApiModels.AccountResponse;
import com.luxsoft.banking.api.ApiModels.AmountRequest;
import com.luxsoft.banking.api.ApiModels.CreateAccountRequest;
import com.luxsoft.banking.api.ApiModels.LedgerResponse;
import com.luxsoft.banking.api.ApiModels.TransferRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;

/** Defines account operations exposed by the service layer. */
public interface AccountService {
    /** Creates an account and records a non-zero opening balance in its ledger. */
    AccountResponse createAccount(CreateAccountRequest request);

    /** Returns account details and current balance for the account-number primary key. */
    AccountResponse getAccountDetails(String accountNumber);

    /** Credits an account and records the deposit in its ledger. */
    LedgerResponse deposit(String accountNumber, AmountRequest request);

    /** Debits an account when funds are sufficient and records the withdrawal. */
    LedgerResponse withdraw(String accountNumber, AmountRequest request);

    /** Moves funds atomically between distinct accounts and returns the shared ledger reference. */
    UUID transfer(TransferRequest request);

    /** Returns a page of newest-first ledger entries for an existing account. */
    Page<LedgerResponse> history(String accountNumber, int page, int size);
}

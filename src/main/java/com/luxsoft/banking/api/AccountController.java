package com.luxsoft.banking.api;

import com.luxsoft.banking.service.AccountService;
import com.luxsoft.banking.model.ApiModels.AccountResponse;
import com.luxsoft.banking.model.ApiModels.AmountRequest;
import com.luxsoft.banking.model.ApiModels.CreateAccountRequest;
import com.luxsoft.banking.model.ApiModels.LedgerResponse;
import com.luxsoft.banking.model.ApiModels.TransferRequest;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AccountController {
    private final AccountService accountService;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(AccountController.class);

    /**
     * Creates the controller with the account service dependency.
     */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Creates an account and returns its primary-key URL in the Location header.
     */
    @PostMapping("/createAccount")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        logger.info("Account creation requested for account number: {}", request.accountNumber());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.accountNumber())).body(response);
    }

    /**
     * Reads account details by the account-number primary key.
     */
    @GetMapping("/accounts/{accountNumber}")
    public AccountResponse getByAccountNumber(@PathVariable String accountNumber) {
        logger.info("Account details requested for account number: {}", accountNumber);
        return accountService.getAccountDetails(accountNumber);
    }

    /**
     * Deposits the requested amount into the selected account.
     */
    @PostMapping("/accounts/{accountNumber}/deposits")
    public LedgerResponse deposit(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) {
        logger.info("Deposit requested for account number: {} with amount: {}", accountNumber, request.amount());
        return accountService.deposit(accountNumber, request);
    }

    /**
     * Withdraws the requested amount from the selected account.
     */
    @PostMapping("/accounts/{accountNumber}/withdrawals")
    public LedgerResponse withdraw(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) {
        logger.info("Withdrawal requested for account number: {} with amount: {}", accountNumber, request.amount());
        return accountService.withdraw(accountNumber, request);
    }

    /**
     * Transfers funds between accounts and returns the shared ledger reference.
     */
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        logger.info("Transfer requested with amount: {}", request.amount());
        return ResponseEntity.ok(new TransferResponse(accountService.transfer(request)));
    }

    /**
     * Returns a bounded page of the selected account's transaction history.
     */
    @GetMapping("/accounts/{accountNumber}/transactions")
    public Page<LedgerResponse> history(@PathVariable String accountNumber,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        logger.info("Transaction history requested for account number: {} with page: {} and size: {}", accountNumber, page, size);
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        return accountService.history(accountNumber, page, size);
    }

    /**
     * Response body containing the ledger reference shared by both transfer entries.
     */
    public record TransferResponse(UUID referenceId) {
    }
}

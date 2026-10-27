package com.luxsoft.banking.account;

import com.luxsoft.banking.api.ApiModels.*;
import com.luxsoft.banking.transaction.EntryType;
import com.luxsoft.banking.transaction.LedgerEntry;
import com.luxsoft.banking.transaction.LedgerEntryRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ModelMapper modelMapper;

    /**
     * Creates the service with its account and ledger persistence dependencies.
     */
    public AccountServiceImpl(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository,
                              ModelMapper modelMapper) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.modelMapper = modelMapper;
        log.debug("AccountServiceImpl initialized");
    }

    /**
     * Creates an account and records a non-zero opening balance in its ledger.
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Account creation requested");
        if (accountRepository.existsById(request.accountNumber())) {
            log.warn("Account creation rejected because the account number already exists");
            throw new AccountAlreadyExistsException(request.accountNumber());
        }
        Account account = modelMapper.map(request, Account.class);
        account.setIfsc(request.ifsc().toUpperCase(java.util.Locale.ROOT));
        account.initializeForCreation();
        account = accountRepository.save(account);
        if (request.balance().signum() > 0) {
            ledgerEntryRepository.save(new LedgerEntry(account, EntryType.OPENING_BALANCE,
                    request.balance(), UUID.randomUUID()));
        }
        log.info("Account created successfully");
        return modelMapper.map(account, AccountResponse.class);
    }

    /**
     * Returns account details and the current balance for the given primary key.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountDetails(String accountNumber) {
        log.debug("Account details lookup requested for account number: {}", accountNumber);
        return modelMapper.map(findAccount(accountNumber), AccountResponse.class);
    }

    /**
     * Credits an account and records the deposit in its ledger.
     */
    @Transactional
    public LedgerResponse deposit(String accountNumber, AmountRequest request) {
        log.info("Deposit requested for account number: {}", accountNumber);
        Account account = lockAccount(accountNumber);
        account.credit(request.amount());
        accountRepository.save(account);
        LedgerResponse response = modelMapper.map(updateLedger(account, EntryType.DEPOSIT,
                request.amount(), UUID.randomUUID()), LedgerResponse.class);
        log.info("Deposit completed and ledger entry saved");
        return response;
    }

    /**
     * Debits an account when funds are sufficient and records the withdrawal.
     */
    @Transactional
    public LedgerResponse withdraw(String accountNumber, AmountRequest request) {
        log.info("Withdrawal requested for account number: {}", accountNumber);
        Account account = lockAccount(accountNumber);
        if (account.getBalance().subtract(request.amount()).signum() <= 0) {
            log.warn("Withdrawal rejected because it would leave a non-positive balance");
            throw new IllegalArgumentException("Insufficient balance");
        }
        account.debit(request.amount());
        accountRepository.save(account);
        LedgerResponse response = updateLedger(account, EntryType.WITHDRAWAL, request.amount(), UUID.randomUUID());
        log.info("Withdrawal completed and ledger entry saved");
        return response;
    }

    /**
     * Moves funds between two accounts as one transaction and records both ledger sides.
     * Account locks are acquired in account-number order to reduce deadlocks.
     *
     * @return shared reference ID for both transfer ledger entries
     */
    @Transactional
    public UUID transfer(TransferRequest request) {
        log.info("Transfer requested with amount: {}", request.amount());
        log.info("Source account: {}, Destination account: {}", request.sourceAccountNumber(), request.destinationAccountNumber());
        if (request.sourceAccountNumber().equals(request.destinationAccountNumber())) {
            log.warn("Transfer rejected because source and destination accounts are the same");
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        // Lock in a stable order to reduce deadlocks for simultaneous opposite-direction transfers.
        String firstAccountNumber = request.sourceAccountNumber().compareTo(request.destinationAccountNumber()) < 0
                ? request.sourceAccountNumber() : request.destinationAccountNumber();
        String secondAccountNumber = firstAccountNumber.equals(request.sourceAccountNumber())
                ? request.destinationAccountNumber() : request.sourceAccountNumber();
        Account first = lockAccount(firstAccountNumber);
        Account second = lockAccount(secondAccountNumber);
        Account source = first.getAccountNumber().equals(request.sourceAccountNumber()) ? first : second;
        Account destination = first.getAccountNumber().equals(request.destinationAccountNumber()) ? first : second;

        source.debit(request.amount());
        destination.credit(request.amount());
        UUID referenceId = UUID.randomUUID();
        updateLedger(source, EntryType.TRANSFER_OUT, request.amount(), referenceId);
        updateLedger(destination, EntryType.TRANSFER_IN, request.amount(), referenceId);
        log.info("Transfer completed and both ledger entries saved");
        return referenceId;
    }

    /**
     * Returns a page of newest-first ledger entries for an existing account.
     */
    @Transactional(readOnly = true)
    public Page<LedgerResponse> history(String accountNumber, int page, int size) {
        log.info("Account transaction history requested for account number: {} with page: {} and size: {}", accountNumber, page, size);
        findAccount(accountNumber);
        return ledgerEntryRepository.findByAccount_AccountNumberOrderByCreatedAtDescIdDesc(accountNumber,
                        PageRequest.of(page, size, Sort.unsorted().descending()))
                .map(entry -> modelMapper.map(entry, LedgerResponse.class));
    }

    /**
     * Loads an account or throws when its account number is unknown.
     */
    private Account findAccount(String accountNumber) {
        log.trace("Loading account by primary key: {}", accountNumber);
        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    /**
     * Loads an account under a pessimistic write lock.
     */
    private Account lockAccount(String accountNumber) {
        log.trace("Loading account with a write lock");
        log.info("Locking account for update: {}", accountNumber);
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    /**
     * Persists a ledger entry reflecting the account balance after the operation.
     */
    private LedgerResponse updateLedger(Account account, EntryType type, BigDecimal amount, UUID referenceId) {
        log.trace("Saving ledger entry for operation type {}", type);
        log.info("Updating ledger for account number: {} with type: {}, amount: {}, referenceId: {}", account.getAccountNumber(), type, amount, referenceId);
        return modelMapper.map(ledgerEntryRepository.save(new LedgerEntry(account, type, amount, referenceId)),
                LedgerResponse.class);
    }
}

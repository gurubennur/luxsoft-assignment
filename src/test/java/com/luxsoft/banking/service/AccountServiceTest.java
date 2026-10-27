package com.luxsoft.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luxsoft.banking.model.ApiModels.AccountResponse;
import com.luxsoft.banking.model.ApiModels.AmountRequest;
import com.luxsoft.banking.model.ApiModels.CreateAccountRequest;
import com.luxsoft.banking.model.ApiModels.LedgerResponse;
import com.luxsoft.banking.model.ApiModels.TransferRequest;
import com.luxsoft.banking.config.ModelMapperConfiguration;
import com.luxsoft.banking.exception.AccountAlreadyExistsException;
import com.luxsoft.banking.exception.AccountNotFoundException;
import com.luxsoft.banking.model.Account;
import com.luxsoft.banking.model.EntryType;
import com.luxsoft.banking.model.LedgerEntry;
import com.luxsoft.banking.repository.AccountRepository;
import com.luxsoft.banking.repository.LedgerEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock AccountRepository accountRepository;
    @Mock LedgerEntryRepository ledgerEntryRepository;
    private AccountService service;
    private String accountNumber;
    private Account account;

    /** Builds a service with mocked repositories and a consistent account fixture. */
    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(accountRepository, ledgerEntryRepository,
                new ModelMapperConfiguration().modelMapper());
        accountNumber = "1234567890";
        account = new Account(accountNumber, "Test Account Holder", "560001",
                "ABCD0123456", "Main Branch", new BigDecimal("100.00"));
    }

    /** Verifies account creation maps fields and records a positive opening balance. */
    @Test
    void createAccountMapsRequestAndRecordsOpeningBalance() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateAccountRequest request = createRequest("9876543210", new BigDecimal("75.00"));

        AccountResponse response = service.createAccount(request);

        assertEquals("9876543210", response.accountNumber());
        assertEquals("Asha Sharma", response.accountHolderName());
        assertEquals("560001", response.pinCode());
        assertEquals("ABCD0123456", response.ifsc());
        assertEquals("Central Branch", response.branch());
        assertEquals(new BigDecimal("75.00"), response.balance());
        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(ledgerCaptor.capture());
        assertEquals(EntryType.OPENING_BALANCE, ledgerCaptor.getValue().getType());
        assertEquals(new BigDecimal("75.00"), ledgerCaptor.getValue().getAmount());
    }

    /** Rejects duplicate account numbers without writing either account or ledger data. */
    @Test
    void createAccountRejectsDuplicateAccountNumber() {
        when(accountRepository.existsById("9876543210")).thenReturn(true);

        assertThrows(AccountAlreadyExistsException.class,
                () -> service.createAccount(createRequest("9876543210", new BigDecimal("75.00"))));

        verify(accountRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    /** Allows a zero opening balance without creating an invalid zero-amount ledger entry. */
    @Test
    void createAccountWithZeroBalanceSkipsOpeningLedger() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = service.createAccount(createRequest("9876543210", BigDecimal.ZERO));

        assertEquals(BigDecimal.ZERO, response.balance());
        verify(accountRepository).save(any(Account.class));
        verify(ledgerEntryRepository, never()).save(any());
    }

    /** Maps the stored entity into account details. */
    @Test
    void getAccountDetailsReturnsMappedAccount() {
        when(accountRepository.findById(accountNumber)).thenReturn(Optional.of(account));

        AccountResponse response = service.getAccountDetails(accountNumber);

        assertEquals(accountNumber, response.accountNumber());
        assertEquals("Test Account Holder", response.accountHolderName());
        assertEquals(new BigDecimal("100.00"), response.balance());
    }

    /** Reports an unknown account when reading account details. */
    @Test
    void getAccountDetailsRejectsUnknownAccount() {
        when(accountRepository.findById(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getAccountDetails(accountNumber));
    }

    /** Credits the account and persists the account before its ledger entry. */
    @Test
    void depositUpdatesAccountAndRecordsLedgerEntry() {
        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerResponse response = service.deposit(accountNumber, new AmountRequest(new BigDecimal("25.00")));

        assertEquals(new BigDecimal("125.00"), account.getBalance());
        assertEquals(new BigDecimal("25.00"), response.getAmount());
        assertEquals(new BigDecimal("125.00"), response.getBalanceAfter());
        assertEquals("DEPOSIT", response.getType());
        InOrder saves = inOrder(accountRepository, ledgerEntryRepository);
        saves.verify(accountRepository).save(account);
        saves.verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    /** Does not modify an account or ledger when depositing to an unknown account. */
    @Test
    void depositRejectsUnknownAccount() {
        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.deposit(accountNumber, new AmountRequest(new BigDecimal("10.00"))));

        verify(accountRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    /** Debits the account and records its resulting balance. */
    @Test
    void withdrawalUpdatesAccountAndRecordsLedgerEntry() {
        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerResponse response = service.withdraw(accountNumber, new AmountRequest(new BigDecimal("35.00")));

        assertEquals(new BigDecimal("65.00"), account.getBalance());
        assertEquals(new BigDecimal("65.00"), response.getBalanceAfter());
        assertEquals("WITHDRAWAL", response.getType());
        verify(accountRepository).save(account);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    /** Rejects an overdraft without changing or persisting account and ledger data. */
    @Test
    void withdrawalRejectsInsufficientFunds() {
        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.withdraw(accountNumber, new AmountRequest(new BigDecimal("100.01"))));

        assertEquals("Insufficient balance", exception.getMessage());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verify(accountRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    /** Rejects a withdrawal that would leave exactly zero balance. */
    @Test
    void withdrawalRejectsZeroRemainingBalance() {
        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class,
                () -> service.withdraw(accountNumber, new AmountRequest(new BigDecimal("100.00"))));

        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verify(accountRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    /** Rejects a withdrawal when its account does not exist. */
    @Test
    void withdrawalRejectsUnknownAccount() {
        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.withdraw(accountNumber, new AmountRequest(new BigDecimal("10.00"))));

        verify(accountRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    /** Transfers balances and persists both ledger sides with one shared reference ID. */
    @Test
    void transferUpdatesBothAccountsAndRecordsBothLedgerEntries() {
        Account destination = destinationAccount();
        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumberForUpdate(destination.getAccountNumber()))
                .thenReturn(Optional.of(destination));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID referenceId = service.transfer(new TransferRequest(accountNumber, destination.getAccountNumber(),
                new BigDecimal("30.00")));

        assertEquals(new BigDecimal("70.00"), account.getBalance());
        assertEquals(new BigDecimal("50.00"), destination.getBalance());
        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(ledgerCaptor.capture());
        List<LedgerEntry> entries = ledgerCaptor.getAllValues();
        assertEquals(EntryType.TRANSFER_OUT, entries.get(0).getType());
        assertEquals(EntryType.TRANSFER_IN, entries.get(1).getType());
        assertEquals(referenceId, entries.get(0).getReferenceId());
        assertEquals(referenceId, entries.get(1).getReferenceId());
        assertEquals(new BigDecimal("70.00"), entries.get(0).getBalanceAfter());
        assertEquals(new BigDecimal("50.00"), entries.get(1).getBalanceAfter());
    }

    /** Rejects transfers that use one account as both source and destination. */
    @Test
    void transferRejectsSameAccount() {
        assertThrows(IllegalArgumentException.class, () -> service.transfer(
                new TransferRequest(accountNumber, accountNumber, new BigDecimal("10.00"))));

        verify(accountRepository, never()).findByAccountNumberForUpdate(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    /** Does not transfer or write ledger entries when source funds would become zero. */
    @Test
    void transferRejectsInsufficientFunds() {
        Account destination = destinationAccount();
        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumberForUpdate(destination.getAccountNumber()))
                .thenReturn(Optional.of(destination));

        assertThrows(IllegalArgumentException.class, () -> service.transfer(new TransferRequest(
                accountNumber, destination.getAccountNumber(), new BigDecimal("100.00"))));

        assertEquals(new BigDecimal("100.00"), account.getBalance());
        assertEquals(new BigDecimal("20.00"), destination.getBalance());
        verify(ledgerEntryRepository, never()).save(any());
    }

    /** Rejects a transfer when either locked account cannot be found. */
    @Test
    void transferRejectsMissingAccount() {
        String destination = "9876543210";
        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumberForUpdate(destination)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.transfer(new TransferRequest(
                accountNumber, destination, new BigDecimal("10.00"))));

        verify(ledgerEntryRepository, never()).save(any());
    }

    /** Maps ledger history entries into response models for the requested page. */
    @Test
    void historyReturnsMappedLedgerPage() {
        LedgerEntry entry = new LedgerEntry(account, EntryType.DEPOSIT,
                new BigDecimal("10.00"), UUID.randomUUID());
        when(accountRepository.findById(accountNumber)).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.findByAccount_AccountNumberOrderByCreatedAtDescIdDesc(
                eq(accountNumber), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entry)));

        Page<LedgerResponse> response = service.history(accountNumber, 0, 5);

        assertEquals(1, response.getTotalElements());
        assertEquals("DEPOSIT", response.getContent().get(0).getType());
        assertEquals(accountNumber, response.getContent().get(0).getAccountNumber());
        verify(ledgerEntryRepository).findByAccount_AccountNumberOrderByCreatedAtDescIdDesc(
                eq(accountNumber), any(Pageable.class));
    }

    /** Does not query history for an account that does not exist. */
    @Test
    void historyRejectsUnknownAccount() {
        when(accountRepository.findById(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.history(accountNumber, 0, 5));

        verify(ledgerEntryRepository, never()).findByAccount_AccountNumberOrderByCreatedAtDescIdDesc(
                eq(accountNumber), any(Pageable.class));
    }

    /** Builds a representative valid account-creation request. */
    private CreateAccountRequest createRequest(String number, BigDecimal balance) {
        return new CreateAccountRequest(number, "Asha Sharma", "560001", "abcd0123456",
                "Central Branch", balance);
    }

    /** Builds the destination account used by transfer scenarios. */
    private Account destinationAccount() {
        return new Account("9876543210", "Destination", "560002", "EFGH0123456",
                "South Branch", new BigDecimal("20.00"));
    }
}

package com.luxsoft.banking.transaction;

import com.luxsoft.banking.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_number", referencedColumnName = "account_number", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false, updatable = false)
    private UUID referenceId;

    /** Required by JPA when loading a ledger entry from the database. */
    protected LedgerEntry() { }

    /**
     * Creates an immutable ledger record for an account operation.
     *
     * @param account account whose balance changed
     * @param type operation represented by this ledger side
     * @param amount positive transaction amount
     * @param referenceId operation ID shared by related ledger entries
     */
    public LedgerEntry(Account account, EntryType type, BigDecimal amount, UUID referenceId) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = account.getBalance();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.referenceId = referenceId;
    }

    /** @return generated ledger-entry identifier */
    public Long getId() { return id; }
    /** @return account number associated with this entry */
    public String getAccountNumber() { return account.getAccountNumber(); }
    /** @return operation represented by this entry */
    public EntryType getType() { return type; }
    /** @return absolute transaction amount */
    public BigDecimal getAmount() { return amount; }
    /** @return account balance immediately after this entry */
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    /** @return UTC timestamp when this entry was created */
    public Instant getCreatedAt() { return createdAt; }
    /** @return UTC timestamp when this entry was last updated */
    public Instant getUpdatedAt() { return updatedAt; }
    /** @return identifier shared by entries created for the same operation */
    public UUID getReferenceId() { return referenceId; }

    /** Initializes missing timestamps before JPA inserts this entry. */
    @PrePersist
    private void initializeTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    /** Refreshes the update timestamp before JPA updates this entry. */
    @PreUpdate
    private void refreshUpdatedAt() { updatedAt = Instant.now(); }
}

package com.luxsoft.banking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @Column(name = "account_number", nullable = false, length = 34, updatable = false)
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false, length = 120)
    private String accountHolderName;

    @Column(name = "account_open_date", nullable = false, updatable = false)
    private LocalDate accountOpenDate;

    @Column(name = "pin_code", nullable = false, length = 20)
    private String pinCode;

    @Column(nullable = false, length = 11)
    private String ifsc;

    @Column(nullable = false, length = 120)
    private String branch;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "modified_by", nullable = false, length = 120)
    private String modifiedBy;

    @Column(name = "updated_by", nullable = false, length = 120)
    private String updatedBy;

    @Version
    private long version;

    /** Required by JPA when loading an account from the database. */
    public Account() { }

    /**
     * Creates an account with its opening details and system audit metadata.
     *
     * @param accountNumber unique account number and primary key
     * @param accountHolderName name of the account holder
     * @param pinCode postal code for the account holder
     * @param ifsc normalized IFSC for the servicing branch
     * @param branch name of the servicing branch
     * @param openingBalance initial non-negative balance
     */
    public Account(String accountNumber, String accountHolderName, String pinCode,
                   String ifsc, String branch, BigDecimal openingBalance) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.accountOpenDate = LocalDate.now();
        this.pinCode = pinCode;
        this.ifsc = ifsc;
        this.branch = branch;
        this.balance = openingBalance;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.modifiedBy = "system";
        this.updatedBy = "system";
    }

    /** @return account number, which is also the account primary key */
    public String getAccountNumber() { return accountNumber; }
    /** @return account holder's name */
    public String getAccountHolderName() { return accountHolderName; }
    /** @return date the account was opened */
    public LocalDate getAccountOpenDate() { return accountOpenDate; }
    /** @return account holder's postal code */
    public String getPinCode() { return pinCode; }
    /** @return IFSC of the servicing branch */
    public String getIfsc() { return ifsc; }
    /** @return name of the servicing branch */
    public String getBranch() { return branch; }
    /** @return current account balance */
    public BigDecimal getBalance() { return balance; }
    /** @return time the account record was created */
    public Instant getCreatedAt() { return createdAt; }
    /** @return time the account record was last updated */
    public Instant getUpdatedAt() { return updatedAt; }
    /** @return actor recorded as the modifier */
    public String getModifiedBy() { return modifiedBy; }
    /** @return actor recorded as the latest updater */
    public String getUpdatedBy() { return updatedBy; }

    /** Sets the initial balance while mapping a validated account-creation request. */
    void setBalance(BigDecimal balance) { this.balance = balance; }

    /** Replaces the IFSC with the normalized value supplied by the service. */
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }

    /** Adds a validated amount to the account balance. */
    public void credit(BigDecimal amount) { balance = balance.add(amount); }

    /**
     * Subtracts an amount only when the resulting balance stays above zero.
     *
     * @throws IllegalArgumentException if the withdrawal would overdraw the account
     */
    public void debit(BigDecimal amount) {
        BigDecimal remainingBalance = balance.subtract(amount);
        if (remainingBalance.signum() <= 0) {
            throw new IllegalArgumentException("Withdrawal must leave a positive balance");
        }
        balance = remainingBalance;
    }

    /** Initializes missing creation and audit values before JPA inserts this entity. */
    @PrePersist
    private void initializeAuditFields() {
        Instant now = Instant.now();
        if (accountOpenDate == null) accountOpenDate = LocalDate.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (modifiedBy == null) modifiedBy = "system";
        if (updatedBy == null) updatedBy = "system";
    }

    /** Initializes creation-time dates and actor fields before an account is persisted. */
    public void initializeForCreation() { initializeAuditFields(); }

    /** Refreshes update time and system actor fields before JPA updates this entity. */
    @PreUpdate
    private void refreshAuditFields() {
        updatedAt = Instant.now();
        modifiedBy = "system";
        updatedBy = "system";
    }
}

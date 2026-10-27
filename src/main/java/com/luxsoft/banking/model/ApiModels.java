package com.luxsoft.banking.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ApiModels {
    /**
     * Prevents instances of this holder class; API payloads are represented by records.
     */
    private ApiModels() {
    }

    /**
     * Validated payload used to open an account.
     */
    public record CreateAccountRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,34}") String accountNumber,
            @NotBlank @Size(max = 120) String accountHolderName,
            @NotBlank @Size(max = 20) String pinCode,
            @NotBlank @Pattern(regexp = "(?i)[A-Z]{4}0[A-Z0-9]{6}", message = "must be a valid 11-character IFSC") String ifsc,
            @NotBlank @Size(max = 120) String branch,
            @JsonProperty("openingBalance")
            @NotNull @DecimalMin(value = "0.00", inclusive = true) @Digits(integer = 17, fraction = 2)
            BigDecimal balance) {
    }

    /**
     * Validated positive amount used by a deposit or withdrawal.
     */
    public record AmountRequest(
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2)
            BigDecimal amount) {
    }

    /**
     * Validated details for a transfer between two distinct account numbers.
     */
    public record TransferRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,34}") String sourceAccountNumber,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,34}") String destinationAccountNumber,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2)
            BigDecimal amount) {
    }

    /**
     * Public representation of an account and its current audit metadata.
     */
    public record AccountResponse(String accountNumber, String accountHolderName,
                                  LocalDate accountOpenDate, String pinCode, String ifsc, String branch,
                                  BigDecimal balance, Instant createdAt, Instant updatedAt,
                                  String modifiedBy, String updatedBy) {
    }

    /**
     * Mutable response model used by ModelMapper for one account-side ledger entry.
     */
    public static class LedgerResponse {
        private Long id;
        private String accountNumber;
        private String type;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID referenceId;

        /**
         * Creates an empty response for ModelMapper.
         */
        public LedgerResponse() {
        }

        /**
         * @return generated ledger-entry identifier
         */
        public Long getId() {
            return id;
        }

        /**
         * @param id generated ledger-entry identifier
         */
        public void setId(Long id) {
            this.id = id;
        }

        /**
         * @return associated account number
         */
        public String getAccountNumber() {
            return accountNumber;
        }

        /**
         * @param accountNumber associated account number
         */
        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        /**
         * @return operation type
         */
        public String getType() {
            return type;
        }

        /**
         * @param type operation type
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * @return transaction amount
         */
        public BigDecimal getAmount() {
            return amount;
        }

        /**
         * @param amount transaction amount
         */
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        /**
         * @return account balance after the operation
         */
        public BigDecimal getBalanceAfter() {
            return balanceAfter;
        }

        /**
         * @param balanceAfter account balance after the operation
         */
        public void setBalanceAfter(BigDecimal balanceAfter) {
            this.balanceAfter = balanceAfter;
        }

        /**
         * @return transaction creation timestamp
         */
        public Instant getCreatedAt() {
            return createdAt;
        }

        /**
         * @param createdAt transaction creation timestamp
         */
        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        /**
         * @return transaction last-update timestamp
         */
        public Instant getUpdatedAt() {
            return updatedAt;
        }

        /**
         * @param updatedAt transaction last-update timestamp
         */
        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }

        /**
         * @return shared operation reference
         */
        public UUID getReferenceId() {
            return referenceId;
        }

        /**
         * @param referenceId shared operation reference
         */
        public void setReferenceId(UUID referenceId) {
            this.referenceId = referenceId;
        }
    }

    /**
     * Common error payload returned by the REST exception handler.
     */
    public record ErrorResponse(String error, String message, Instant timestamp) {
    }
}

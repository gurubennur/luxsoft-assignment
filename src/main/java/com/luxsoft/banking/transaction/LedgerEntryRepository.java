package com.luxsoft.banking.transaction;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    /**
     * Retrieves an account's ledger entries in reverse chronological order.
     *
     * @param accountNumber account primary key to filter by
     * @param pageable requested page and page size
     * @return one page of matching ledger entries
     */
    Page<LedgerEntry> findByAccount_AccountNumberOrderByCreatedAtDescIdDesc(String accountNumber, Pageable pageable);

    /**
     * Finds all ledger sides associated with one operation or transfer.
     *
     * @param referenceId shared operation reference
     * @return matching ledger entries
     */
    List<LedgerEntry> findByReferenceId(UUID referenceId);
}

package com.assignment.wallet_transaction_processor.repository;

import com.assignment.wallet_transaction_processor.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionId(UUID transactionId);
}
package com.assignment.wallet_transaction_processor.dto;

import com.assignment.wallet_transaction_processor.enums.TransactionStatus;
import com.assignment.wallet_transaction_processor.enums.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionResponse {

    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal balance;
    private String message;

    public TransactionResponse(
            UUID transactionId,
            UUID userId,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status,
            BigDecimal balance,
            String message
    ) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.balance = balance;
        this.message = message;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getMessage() {
        return message;
    }
}
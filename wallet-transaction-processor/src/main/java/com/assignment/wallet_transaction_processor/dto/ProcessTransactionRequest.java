package com.assignment.wallet_transaction_processor.dto;

import com.assignment.wallet_transaction_processor.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class ProcessTransactionRequest {

    @NotNull
    private UUID transactionId;

    @NotNull
    private UUID userId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    public ProcessTransactionRequest() {
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
}
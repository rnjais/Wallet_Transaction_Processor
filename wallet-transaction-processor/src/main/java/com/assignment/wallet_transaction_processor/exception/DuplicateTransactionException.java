package com.assignment.wallet_transaction_processor.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String message) {
        super(message);
    }
}
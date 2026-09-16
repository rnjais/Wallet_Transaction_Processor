package com.assignment.wallet_transaction_processor.service;

import org.springframework.stereotype.Service;
import com.assignment.wallet_transaction_processor.dto.ProcessTransactionRequest;
import com.assignment.wallet_transaction_processor.dto.TransactionResponse;
import com.assignment.wallet_transaction_processor.repository.WalletRepository;
import com.assignment.wallet_transaction_processor.repository.TransactionRepository;
import com.assignment.wallet_transaction_processor.entity.Wallet;
import org.springframework.transaction.annotation.Transactional;
import com.assignment.wallet_transaction_processor.entity.Transaction;
import com.assignment.wallet_transaction_processor.enums.TransactionStatus;
import java.time.LocalDateTime;

@Service
public class TransactionService {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse processTransaction(ProcessTransactionRequest request) {

        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Transaction existingTransaction = transactionRepository
                .findByTransactionId(request.getTransactionId())
                .orElse(null);

        if (existingTransaction != null) {
            throw new RuntimeException("Transaction already processed");
        }

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        wallet.setBalance(
                wallet.getBalance().subtract(request.getAmount())
        );

        walletRepository.save(wallet);
        

        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getUserId(),
                request.getAmount(),
                request.getType(),
                TransactionStatus.SUCCESS,
                LocalDateTime.now()
        );

        transactionRepository.save(transaction);

        return new TransactionResponse(
                request.getTransactionId(),
                request.getUserId(),
                request.getAmount(),
                request.getType(),
                TransactionStatus.SUCCESS,
                wallet.getBalance(),
                "Transaction processed successfully"
        );

    }
}
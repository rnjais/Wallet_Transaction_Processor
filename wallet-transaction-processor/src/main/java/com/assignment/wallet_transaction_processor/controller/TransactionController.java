package com.assignment.wallet_transaction_processor.controller;

import com.assignment.wallet_transaction_processor.dto.ProcessTransactionRequest;
import com.assignment.wallet_transaction_processor.dto.TransactionResponse;
import com.assignment.wallet_transaction_processor.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processTransaction(
            @Valid @RequestBody ProcessTransactionRequest request
    ) {
        TransactionResponse response =
                transactionService.processTransaction(request);

        return ResponseEntity.ok(response);
    }
}
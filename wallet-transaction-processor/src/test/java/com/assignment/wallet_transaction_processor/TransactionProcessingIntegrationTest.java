package com.assignment.wallet_transaction_processor;

import com.assignment.wallet_transaction_processor.entity.Wallet;
import com.assignment.wallet_transaction_processor.repository.TransactionRepository;
import com.assignment.wallet_transaction_processor.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionProcessingIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MockMvc mockMvc;


    @Test
    @Transactional
    @DisplayName("Processes a single valid debit transaction successfully.")
    void processesSingleValidDebitTransactionSuccessfully() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Wallet wallet = new Wallet(
                userId,
                new BigDecimal("500.00")
        );

        walletRepository.saveAndFlush(wallet);

        String requestJson = """
                {
                    "transactionId": "%s",
                    "userId": "%s",
                    "amount": 100.00,
                    "type": "DEBIT"
                }
                """.formatted(transactionId, userId);

        System.out.println();
        System.out.println(
                "TEST: Processes a single valid debit transaction successfully."
        );

        mockMvc.perform(
                        post("/api/v1/transactions/process")
                                .contentType(APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.balance").value(400.00));

        Wallet updatedWallet = walletRepository
                .findByUserId(userId)
                .orElseThrow();

        System.out.println(
                "RESULT: Transaction processed successfully."
        );
        System.out.println(
                "INITIAL BALANCE: ₹500.00"
        );
        System.out.println(
                "DEBIT AMOUNT: ₹100.00"
        );
        System.out.println(
                "FINAL BALANCE: ₹" + updatedWallet.getBalance()
        );

        assertEquals(
                new BigDecimal("400.00"),
                updatedWallet.getBalance()
        );
    }


    @Test
    @DisplayName("Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.")
    void processesIdenticalTransactionsOnlyOnce() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Wallet wallet = new Wallet(
                userId,
                new BigDecimal("1000.00")
        );

        walletRepository.saveAndFlush(wallet);

        String requestJson = """
                {
                    "transactionId": "%s",
                    "userId": "%s",
                    "amount": 250.00,
                    "type": "DEBIT"
                }
                """.formatted(transactionId, userId);

        System.out.println();
        System.out.println(
                "TEST: Sends 3 identical transactionIDs simultaneously."
        );
        System.out.println(
                "INTENT: Ensure the balance is deducted only once."
        );

        ExecutorService executorService =
                Executors.newFixedThreadPool(3);

        CountDownLatch readyLatch =
                new CountDownLatch(3);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Callable<Integer>> tasks =
                new ArrayList<>();

        for (int i = 0; i < 3; i++) {

            tasks.add(() -> {

                readyLatch.countDown();

                readyLatch.await();

                startLatch.await();

                return mockMvc.perform(
                                post("/api/v1/transactions/process")
                                        .contentType(APPLICATION_JSON)
                                        .content(requestJson)
                        )
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
        }

        List<Future<Integer>> futures =
                new ArrayList<>();

        for (Callable<Integer> task : tasks) {
            futures.add(executorService.submit(task));
        }

        readyLatch.await();

        startLatch.countDown();

        int successCount = 0;
        int conflictCount = 0;

        for (Future<Integer> future : futures) {

            int statusCode = future.get();

            if (statusCode == 200) {
                successCount++;
            }

            if (statusCode == 409) {
                conflictCount++;
            }
        }

        executorService.shutdown();

        Wallet updatedWallet = walletRepository
                .findById(wallet.getId())
                .orElseThrow();

        System.out.println(
                "RESULT: Concurrent requests completed."
        );
        System.out.println(
                "TOTAL REQUESTS: 3"
        );
        System.out.println(
                "SUCCESSFUL REQUESTS: " + successCount
        );
        System.out.println(
                "DUPLICATE REQUESTS: " + conflictCount
        );
        System.out.println(
                "FINAL BALANCE: ₹" + updatedWallet.getBalance()
        );

        assertEquals(
                1,
                successCount
        );

        assertEquals(
                2,
                conflictCount
        );

        assertEquals(
                new BigDecimal("750.00"),
                updatedWallet.getBalance()
        );
    }
    @Test
    @DisplayName("Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.")
    void processesConcurrentDebitsWithoutNegativeBalance() throws Exception {

        UUID userId = UUID.randomUUID();

        Wallet wallet = new Wallet(
                userId,
                new BigDecimal("500.00")
        );

        walletRepository.saveAndFlush(wallet);

        System.out.println();
        System.out.println(
                "TEST: Sends 10 concurrent debit requests of ₹100."
        );
        System.out.println(
                "INTENT: Ensure final balance is ₹0 and 5 requests fail."
        );

        ExecutorService executorService =
                Executors.newFixedThreadPool(10);

        CountDownLatch readyLatch =
                new CountDownLatch(10);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Callable<Integer>> tasks =
                new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            UUID transactionId = UUID.randomUUID();

            String requestJson = """
                {
                    "transactionId": "%s",
                    "userId": "%s",
                    "amount": 100.00,
                    "type": "DEBIT"
                }
                """.formatted(transactionId, userId);

            tasks.add(() -> {

                readyLatch.countDown();

                readyLatch.await();

                startLatch.await();

                return mockMvc.perform(
                                post("/api/v1/transactions/process")
                                        .contentType(APPLICATION_JSON)
                                        .content(requestJson)
                        )
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
        }

        List<Future<Integer>> futures =
                new ArrayList<>();

        for (Callable<Integer> task : tasks) {
            futures.add(executorService.submit(task));
        }

        readyLatch.await();

        startLatch.countDown();

        int successCount = 0;
        int insufficientFundsCount = 0;

        for (Future<Integer> future : futures) {

            int statusCode = future.get();

            if (statusCode == 200) {
                successCount++;
            }

            if (statusCode == 400) {
                insufficientFundsCount++;
            }
        }

        executorService.shutdown();

        Wallet updatedWallet = walletRepository
                .findById(wallet.getId())
                .orElseThrow();

        System.out.println(
                "RESULT: Concurrent debit requests completed."
        );
        System.out.println(
                "TOTAL REQUESTS: 10"
        );
        System.out.println(
                "SUCCESSFUL REQUESTS: " + successCount
        );
        System.out.println(
                "INSUFFICIENT FUNDS: " + insufficientFundsCount
        );
        System.out.println(
                "FINAL BALANCE: ₹" + updatedWallet.getBalance()
        );

        assertEquals(
                5,
                successCount
        );

        assertEquals(
                5,
                insufficientFundsCount
        );

        assertEquals(
                new BigDecimal("0.00"),
                updatedWallet.getBalance()
        );
    }
}
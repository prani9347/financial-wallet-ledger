package com.praneeth;

import com.praneeth.config.DatabaseConnection;
import com.praneeth.service.TransferService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentTransferTest {

    @Test
    void testConcurrentTransfers() throws Exception {

        TransferService transferService = new TransferService();

        long fromWalletId = 1;
        long toWalletId = 2;
        BigDecimal transferAmount = new BigDecimal("100.00");
        int numberOfTransfers = 50;

        // Reset sender wallet to a known balance before the test
        resetWalletBalance(fromWalletId, new BigDecimal("1000.00"));
        resetWalletBalance(toWalletId, new BigDecimal("0.00"));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch endSignal = new CountDownLatch(numberOfTransfers);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfTransfers);

        Runnable task = () -> {
            try {
                startSignal.await();

                boolean result = transferService.transfer(
                        fromWalletId, toWalletId, transferAmount
                );

                if (result) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endSignal.countDown();
            }
        };

        for (int i = 0; i < numberOfTransfers; i++) {
            executor.submit(task);
        }

        startSignal.countDown();
        endSignal.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal finalSenderBalance = getWalletBalance(fromWalletId);
        BigDecimal finalReceiverBalance = getWalletBalance(toWalletId);

        System.out.println("=================================");
        System.out.println("TOTAL ATTEMPTS      = " + numberOfTransfers);
        System.out.println("SUCCESS COUNT       = " + successCount.get());
        System.out.println("FAILED COUNT        = " + failCount.get());
        System.out.println("FINAL SENDER BAL    = " + finalSenderBalance);
        System.out.println("FINAL RECEIVER BAL  = " + finalReceiverBalance);
        System.out.println("=================================");
    }

    private void resetWalletBalance(long walletId, BigDecimal balance) throws Exception {
        String sql = "UPDATE wallet SET balance = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, balance);
            statement.setLong(2, walletId);
            statement.executeUpdate();
        }
    }

    private BigDecimal getWalletBalance(long walletId) throws Exception {
        String sql = "SELECT balance FROM wallet WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, walletId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("balance");
            }
        }
    }
}
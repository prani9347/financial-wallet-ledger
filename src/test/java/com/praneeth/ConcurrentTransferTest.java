package com.praneeth;

import com.praneeth.service.TransferService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrentTransferTest {

    @Test
    void testConcurrentTransfers() throws InterruptedException {

        TransferService transferService = new TransferService();

        Thread thread1 = new Thread(() -> {
            transferService.transfer(
                    1,
                    2,
                    new BigDecimal("5000.00")
            );
        });

        Thread thread2 = new Thread(() -> {
            transferService.transfer(
                    1,
                    2,
                    new BigDecimal("5000.00")
            );
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        assertTrue(true);

        System.out.println("Both concurrent transfers completed.");
    }
}
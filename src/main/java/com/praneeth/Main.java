package com.praneeth;

import com.praneeth.repository.WalletRepository;
import com.praneeth.service.TransferService;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {

        WalletRepository repository = new WalletRepository();

        repository.createWallet(
                "Rahul",
                new BigDecimal("5000.00")
        );

        TransferService transferService = new TransferService();

        transferService.transfer(
                1,
                2,
                new BigDecimal("2000.00")
        );
    }
}
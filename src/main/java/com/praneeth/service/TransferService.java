package com.praneeth.service;

import com.praneeth.config.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TransferService {

    public boolean transfer(long fromWalletId, long toWalletId, BigDecimal amount) {

        String selectWalletSql = """
                SELECT balance
                FROM wallet
                WHERE id = ?
                FOR UPDATE
                """;

        String debitSql = """
                UPDATE wallet
                SET balance = balance - ?
                WHERE id = ?
                """;

        String creditSql = """
                UPDATE wallet
                SET balance = balance + ?
                WHERE id = ?
                """;

        String ledgerSql = """
                INSERT INTO ledger_transaction
                (wallet_id, type, amount, description)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // Always lock wallets in ascending ID order.
                long firstWalletId = Math.min(fromWalletId, toWalletId);
                long secondWalletId = Math.max(fromWalletId, toWalletId);

                // Lock first wallet
                lockWallet(connection, selectWalletSql, firstWalletId);

                // Lock second wallet
                if (secondWalletId != firstWalletId) {
                    lockWallet(connection, selectWalletSql, secondWalletId);
                }

                // Check sender balance
                BigDecimal senderBalance = getWalletBalance(
                        connection,
                        selectWalletSql,
                        fromWalletId
                );

                if (senderBalance.compareTo(amount) < 0) {
                    throw new Exception("Insufficient balance.");
                }

                // Debit sender
                try (PreparedStatement statement =
                             connection.prepareStatement(debitSql)) {

                    statement.setBigDecimal(1, amount);
                    statement.setLong(2, fromWalletId);

                    statement.executeUpdate();
                }

                // Credit receiver
                try (PreparedStatement statement =
                             connection.prepareStatement(creditSql)) {

                    statement.setBigDecimal(1, amount);
                    statement.setLong(2, toWalletId);

                    statement.executeUpdate();
                }

                // Debit ledger entry
                try (PreparedStatement statement =
                             connection.prepareStatement(ledgerSql)) {

                    statement.setLong(1, fromWalletId);
                    statement.setString(2, "DEBIT");
                    statement.setBigDecimal(3, amount);
                    statement.setString(4, "Transfer to wallet " + toWalletId);

                    statement.executeUpdate();
                }

                // Credit ledger entry
                try (PreparedStatement statement =
                             connection.prepareStatement(ledgerSql)) {

                    statement.setLong(1, toWalletId);
                    statement.setString(2, "CREDIT");
                    statement.setBigDecimal(3, amount);
                    statement.setString(4, "Transfer from wallet " + fromWalletId);

                    statement.executeUpdate();
                }

                connection.commit();

                System.out.println("Transfer successful.");
                return true;

            } catch (Exception e) {

                connection.rollback();

                System.out.println("Transfer failed.");
                System.out.println("ROLLBACK executed.");
                System.out.println("Reason: " + e.getMessage());
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void lockWallet(
            Connection connection,
            String sql,
            long walletId
    ) throws Exception {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, walletId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new Exception(
                            "Wallet not found: " + walletId
                    );
                }
            }
        }
    }

    private BigDecimal getWalletBalance(
            Connection connection,
            String sql,
            long walletId
    ) throws Exception {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, walletId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new Exception(
                            "Wallet not found: " + walletId
                    );
                }

                return resultSet.getBigDecimal("balance");
            }
        }
    }
}
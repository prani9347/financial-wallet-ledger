package com.praneeth.repository;

import com.praneeth.config.DatabaseConnection;
import com.praneeth.model.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class WalletRepository {

    public Wallet createWallet(String ownerName, BigDecimal initialBalance) {

        String sql = """
                INSERT INTO wallet (owner_name, balance)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, ownerName);
            statement.setBigDecimal(2, initialBalance);

            statement.executeUpdate();

            ResultSet resultSet = statement.getGeneratedKeys();

            if (resultSet.next()) {
                long id = resultSet.getLong(1);

                return new Wallet(id, ownerName, initialBalance);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void credit(long walletId, BigDecimal amount) {

        String sql = """
                UPDATE wallet
                SET balance = balance + ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, amount);
            statement.setLong(2, walletId);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                System.out.println("Credit failed. Wallet not found.");
            } else {
                System.out.println("Credit successful.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void debit(long walletId, BigDecimal amount) {

        String sql = """
                UPDATE wallet
                SET balance = balance - ?
                WHERE id = ?
                  AND balance >= ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, amount);
            statement.setLong(2, walletId);
            statement.setBigDecimal(3, amount);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                System.out.println("Debit failed. Insufficient balance or wallet not found.");
            } else {
                System.out.println("Debit successful.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
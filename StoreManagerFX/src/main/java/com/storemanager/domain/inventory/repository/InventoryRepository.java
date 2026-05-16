package com.storemanager.domain.inventory.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class InventoryRepository {

    public InventoryRepository() {

        initializeTables();
    }

    public List<InventoryItem> findAllItems() {

        List<InventoryItem> items =
                new ArrayList<>();

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement();

                ResultSet resultSet =
                        statement.executeQuery(
                                """
                                SELECT *
                                FROM inventory_items
                                ORDER BY product_id
                                """
                        )
        ) {

            while (resultSet.next()) {
                items.add(
                        mapItem(resultSet)
                );
            }

            return items;

        } catch (Exception e) {

            e.printStackTrace();

            return items;
        }
    }

    public List<InventoryTransaction> findAllTransactions() {

        List<InventoryTransaction> transactions =
                new ArrayList<>();

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement();

                ResultSet resultSet =
                        statement.executeQuery(
                                """
                                SELECT *
                                FROM inventory_transactions
                                ORDER BY created_at DESC, id DESC
                                """
                        )
        ) {

            while (resultSet.next()) {
                transactions.add(
                        mapTransaction(resultSet)
                );
            }

            return transactions;

        } catch (Exception e) {

            e.printStackTrace();

            return transactions;
        }
    }

    public boolean applyTransaction(
            InventoryTransaction transaction,
            int quantityDelta
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection()
        ) {

            connection.setAutoCommit(false);

            try {

                int currentQuantity =
                        findCurrentQuantityForUpdate(
                                connection,
                                transaction.getProductId()
                        );

                int newQuantity =
                        currentQuantity + quantityDelta;

                if (newQuantity < 0) {
                    throw new RuntimeException(
                            "Stock cannot be negative"
                    );
                }

                upsertInventoryItem(
                        connection,
                        transaction.getProductId(),
                        newQuantity
                );

                insertTransaction(
                        connection,
                        transaction
                );

                connection.commit();

                return true;

            } catch (Exception e) {

                connection.rollback();
                throw e;
            }

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    private void initializeTables() {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS inventory_items (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        product_id BIGINT NOT NULL UNIQUE,
                        quantity INT NOT NULL DEFAULT 0,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP
                    )
                    """
            );

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS inventory_transactions (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        product_id BIGINT NOT NULL,
                        type VARCHAR(40) NOT NULL,
                        quantity INT NOT NULL,
                        reason VARCHAR(255),
                        created_by_user_id BIGINT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Inventory table initialization failed",
                    e
            );
        }
    }

    private int findCurrentQuantityForUpdate(
            Connection connection,
            Long productId
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT quantity
                                FROM inventory_items
                                WHERE product_id = ?
                                FOR UPDATE
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    productId
            );

            ResultSet resultSet =
                    statement.executeQuery();

            if (!resultSet.next()) {
                return 0;
            }

            return resultSet.getInt("quantity");
        }
    }

    private void upsertInventoryItem(
            Connection connection,
            Long productId,
            int quantity
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO inventory_items (
                                    product_id,
                                    quantity,
                                    updated_at
                                )
                                VALUES (?, ?, CURRENT_TIMESTAMP)
                                ON DUPLICATE KEY UPDATE
                                    quantity = VALUES(quantity),
                                    updated_at = CURRENT_TIMESTAMP
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    productId
            );

            statement.setInt(
                    2,
                    quantity
            );

            statement.executeUpdate();
        }
    }

    private void insertTransaction(
            Connection connection,
            InventoryTransaction transaction
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO inventory_transactions (
                                    product_id,
                                    type,
                                    quantity,
                                    reason,
                                    created_by_user_id
                                )
                                VALUES (?, ?, ?, ?, ?)
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    transaction.getProductId()
            );

            statement.setString(
                    2,
                    transaction.getType().name()
            );

            statement.setInt(
                    3,
                    transaction.getQuantity()
            );

            statement.setString(
                    4,
                    transaction.getReason()
            );

            if (transaction.getCreatedByUserId() == null) {
                statement.setObject(
                        5,
                        null
                );
            } else {
                statement.setLong(
                        5,
                        transaction.getCreatedByUserId()
                );
            }

            statement.executeUpdate();
        }
    }

    private InventoryItem mapItem(
            ResultSet resultSet
    ) throws Exception {

        InventoryItem item =
                new InventoryItem();

        item.setId(resultSet.getLong("id"));
        item.setProductId(resultSet.getLong("product_id"));
        item.setQuantity(resultSet.getInt("quantity"));

        Timestamp updatedAt =
                resultSet.getTimestamp("updated_at");

        if (updatedAt != null) {
            item.setUpdatedAt(
                    updatedAt.toLocalDateTime()
            );
        }

        return item;
    }

    private InventoryTransaction mapTransaction(
            ResultSet resultSet
    ) throws Exception {

        InventoryTransaction transaction =
                new InventoryTransaction();

        transaction.setId(resultSet.getLong("id"));
        transaction.setProductId(resultSet.getLong("product_id"));
        transaction.setType(
                InventoryTransactionType.valueOf(
                        resultSet.getString("type")
                )
        );
        transaction.setQuantity(resultSet.getInt("quantity"));
        transaction.setReason(resultSet.getString("reason"));

        long createdByUserId =
                resultSet.getLong("created_by_user_id");

        if (resultSet.wasNull()) {
            transaction.setCreatedByUserId(null);
        } else {
            transaction.setCreatedByUserId(createdByUserId);
        }

        Timestamp createdAt =
                resultSet.getTimestamp("created_at");

        if (createdAt != null) {
            transaction.setCreatedAt(
                    createdAt.toLocalDateTime()
            );
        }

        return transaction;
    }
}

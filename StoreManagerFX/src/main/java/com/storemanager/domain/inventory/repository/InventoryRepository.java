package com.storemanager.domain.inventory.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
            throw new IllegalStateException("Unable to load records. Check the database connection and try again.", e);
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
            throw new IllegalStateException("Unable to load records. Check the database connection and try again.", e);
        }
    }

    public List<Long> findRecentProductIds(
            int limit
    ) {

        List<Long> productIds =
                new ArrayList<>();

        if (limit <= 0) {
            return productIds;
        }

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT product_id
                                FROM inventory_transactions
                                GROUP BY product_id
                                ORDER BY MAX(created_at) DESC,
                                         MAX(id) DESC
                                LIMIT ?
                                """
                        )
        ) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    productIds.add(resultSet.getLong("product_id"));
                }
            }

            return productIds;

        } catch (Exception e) {
            e.printStackTrace();
            return productIds;
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
                        Math.addExact(currentQuantity, quantityDelta);

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

            throw new RuntimeException(
                    "Cannot apply inventory transaction",
                    e
            );
        }
    }

    public boolean applyTransaction(
            Connection connection,
            InventoryTransaction transaction,
            int quantityDelta
    ) {

        try {
            int currentQuantity =
                    findCurrentQuantityForUpdate(
                            connection,
                            transaction.getProductId()
                    );

            int newQuantity =
                    Math.addExact(currentQuantity, quantityDelta);

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

            return true;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot apply inventory transaction",
                    e
            );
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

            collapseDuplicateInventoryItems(connection);
            addUniqueProductIndexIfMissing(connection);

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Inventory table initialization failed",
                    e
            );
        }
    }

    private void collapseDuplicateInventoryItems(
            Connection connection
    ) throws Exception {

        try (
                Statement statement =
                        connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                """
                                SELECT product_id,
                                       MIN(id) AS keeper_id,
                                       SUM(quantity) AS total_quantity,
                                       COUNT(*) AS row_count
                                FROM inventory_items
                                GROUP BY product_id
                                HAVING COUNT(*) > 1
                                """
                        )
        ) {

            while (resultSet.next()) {
                long productId =
                        resultSet.getLong("product_id");
                long keeperId =
                        resultSet.getLong("keeper_id");
                int totalQuantity =
                        resultSet.getInt("total_quantity");

                updateInventoryItemQuantity(
                        connection,
                        keeperId,
                        totalQuantity
                );
                deleteDuplicateInventoryItems(
                        connection,
                        productId,
                        keeperId
                );
            }
        }
    }

    private void addUniqueProductIndexIfMissing(
            Connection connection
    ) throws Exception {

        DatabaseMetaData metaData =
                connection.getMetaData();

        try (
                ResultSet indexes =
                        metaData.getIndexInfo(
                                null,
                                null,
                                "inventory_items",
                                true,
                                false
                        )
        ) {

            while (indexes.next()) {
                String columnName =
                        indexes.getString("COLUMN_NAME");

                if ("product_id".equalsIgnoreCase(columnName)) {
                    return;
                }
            }
        }

        try (
                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    """
                    ALTER TABLE inventory_items
                    ADD CONSTRAINT uq_inventory_items_product_id
                    UNIQUE (product_id)
                    """
            );
        }
    }

    private void updateInventoryItemQuantity(
            Connection connection,
            long keeperId,
            int quantity
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE inventory_items
                                SET quantity = ?,
                                    updated_at = CURRENT_TIMESTAMP
                                WHERE id = ?
                                """
                        )
        ) {

            statement.setInt(1, quantity);
            statement.setLong(2, keeperId);
            statement.executeUpdate();
        }
    }

    private void deleteDuplicateInventoryItems(
            Connection connection,
            long productId,
            long keeperId
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                DELETE FROM inventory_items
                                WHERE product_id = ?
                                  AND id <> ?
                                """
                        )
        ) {

            statement.setLong(1, productId);
            statement.setLong(2, keeperId);
            statement.executeUpdate();
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

package com.storemanager.domain.importing.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.database.DatabaseInitializer;
import com.storemanager.domain.importing.model.ImportItem;
import com.storemanager.domain.importing.model.ImportReceipt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ImportRepository {

    public Long saveReceipt(
            ImportReceipt receipt,
            List<ImportItem> items
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection()
        ) {

            connection.setAutoCommit(false);

            try {

                Long receiptId =
                        insertReceipt(
                                connection,
                                receipt
                        );

                for (ImportItem item : items) {
                    item.setImportReceiptId(receiptId);
                    insertImportItem(
                            connection,
                            item
                    );
                }

                connection.commit();

                return receiptId;

            } catch (Exception e) {

                connection.rollback();
                throw e;
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot save import receipt",
                    e
            );
        }
    }

    public Long saveReceipt(
            Connection connection,
            ImportReceipt receipt,
            List<ImportItem> items
    ) {

        try {
            Long receiptId =
                    insertReceipt(
                            connection,
                            receipt
                    );

            for (ImportItem item : items) {
                item.setImportReceiptId(receiptId);
                insertImportItem(
                        connection,
                        item
                );
            }

            return receiptId;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot save import receipt",
                    e
            );
        }
    }

    public List<ImportReceipt> findRecentReceipts() {

        List<ImportReceipt> receipts =
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
                                FROM import_receipts
                                ORDER BY created_at DESC, id DESC
                                LIMIT 50
                                """
                        )
        ) {

            while (resultSet.next()) {
                receipts.add(
                        mapReceipt(resultSet)
                );
            }

            return receipts;

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
                                SELECT ii.product_id
                                FROM import_items ii
                                INNER JOIN import_receipts ir
                                        ON ir.id = ii.import_receipt_id
                                GROUP BY ii.product_id
                                ORDER BY MAX(ir.created_at) DESC,
                                         MAX(ii.id) DESC
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

    public BigDecimal findTotalImportCost() {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                Statement statement =
                        connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                """
                                SELECT COALESCE(SUM(total_cost), 0) AS total_import_cost
                                FROM import_receipts
                                """
                        )
        ) {

            if (resultSet.next()) {
                return resultSet.getBigDecimal("total_import_cost");
            }

            return BigDecimal.ZERO;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Cannot load total import cost",
                    e
            );
        }
    }

    public BigDecimal findImportCostForPeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT COALESCE(SUM(total_cost), 0) AS total_import_cost
                                FROM import_receipts
                                WHERE created_at >= ?
                                  AND created_at < ?
                                """
                        )
        ) {

            statement.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(endDate.atStartOfDay()));

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBigDecimal("total_import_cost");
            }

            return BigDecimal.ZERO;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Cannot load import cost for period",
                    e
            );
        }
    }

    public long countReceiptsForPeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT COUNT(*) AS receipt_count
                                FROM import_receipts
                                WHERE created_at >= ?
                                  AND created_at < ?
                                """
                        )
        ) {

            statement.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(endDate.atStartOfDay()));

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getLong("receipt_count");
            }

            return 0L;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Cannot load import receipt count for period",
                    e
            );
        }
    }

    public static void initializeSchema(Connection connection) {

        try (
                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS import_receipts (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        supplier_name VARCHAR(180) NOT NULL,
                        total_cost DECIMAL(18, 2) NOT NULL DEFAULT 0,
                        created_by_user_id BIGINT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS import_items (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        import_receipt_id BIGINT NOT NULL,
                        product_id BIGINT NOT NULL,
                        quantity INT NOT NULL,
                        unit_cost DECIMAL(18, 2) NOT NULL,
                        subtotal DECIMAL(18, 2) NOT NULL
                    )
                    """
            );

            DatabaseInitializer.initializeImportExpiryColumn(connection);

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Import table initialization failed",
                    e
            );
        }
    }

    private Long insertReceipt(
            Connection connection,
            ImportReceipt receipt
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO import_receipts (
                                    supplier_name,
                                    total_cost,
                                    created_by_user_id
                                )
                                VALUES (?, ?, ?)
                                """,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(
                    1,
                    receipt.getSupplierName()
            );
            statement.setBigDecimal(
                    2,
                    receipt.getTotalCost()
            );

            if (receipt.getCreatedByUserId() == null) {
                statement.setObject(
                        3,
                        null
                );
            } else {
                statement.setLong(
                        3,
                        receipt.getCreatedByUserId()
                );
            }

            statement.executeUpdate();

            ResultSet keys =
                    statement.getGeneratedKeys();

            if (!keys.next()) {
                throw new RuntimeException(
                        "Cannot create import receipt"
                );
            }

            return keys.getLong(1);
        }
    }

    private void insertImportItem(
            Connection connection,
            ImportItem item
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO import_items (
                                    import_receipt_id,
                                    product_id,
                                    quantity,
                                    unit_cost,
                                    subtotal,
                                    expiry_date
                                )
                                VALUES (?, ?, ?, ?, ?, ?)
                                """
                        )
        ) {

            statement.setLong(1, item.getImportReceiptId());
            statement.setLong(2, item.getProductId());
            statement.setInt(3, item.getQuantity());
            statement.setBigDecimal(4, item.getUnitCost());
            statement.setBigDecimal(5, item.getSubtotal());
            statement.setDate(6, item.getExpiryDate() == null ? null : java.sql.Date.valueOf(item.getExpiryDate()));
            statement.executeUpdate();
        }
    }

    private ImportReceipt mapReceipt(
            ResultSet resultSet
    ) throws Exception {

        ImportReceipt receipt =
                new ImportReceipt();

        receipt.setId(resultSet.getLong("id"));
        receipt.setSupplierName(resultSet.getString("supplier_name"));
        receipt.setTotalCost(resultSet.getBigDecimal("total_cost"));

        long createdByUserId =
                resultSet.getLong("created_by_user_id");

        if (resultSet.wasNull()) {
            receipt.setCreatedByUserId(null);
        } else {
            receipt.setCreatedByUserId(createdByUserId);
        }

        Timestamp createdAt =
                resultSet.getTimestamp("created_at");

        if (createdAt != null) {
            receipt.setCreatedAt(
                    createdAt.toLocalDateTime()
            );
        }

        return receipt;
    }
}

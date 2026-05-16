package com.storemanager.domain.importing.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.importing.model.ImportItem;
import com.storemanager.domain.importing.model.ImportReceipt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ImportRepository {

    public ImportRepository() {

        initializeTables();
    }

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

            e.printStackTrace();

            return null;
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

            e.printStackTrace();

            return receipts;
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
                                    subtotal
                                )
                                VALUES (?, ?, ?, ?, ?)
                                """
                        )
        ) {

            statement.setLong(1, item.getImportReceiptId());
            statement.setLong(2, item.getProductId());
            statement.setInt(3, item.getQuantity());
            statement.setBigDecimal(4, item.getUnitCost());
            statement.setBigDecimal(5, item.getSubtotal());
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

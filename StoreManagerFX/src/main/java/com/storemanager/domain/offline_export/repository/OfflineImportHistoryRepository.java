package com.storemanager.domain.offline_export.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.offline_export.dto.ImportItemSnapshot;
import com.storemanager.domain.offline_export.dto.ImportReceiptSnapshot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reads the entire import history, rather than the recent-receipts screen's fifty-row limit. */
public class OfflineImportHistoryRepository {

    public List<ImportReceiptSnapshot> findAllReceipts() {
        List<ImportReceiptSnapshot> snapshots = new ArrayList<>();
        try (var connection = ConnectionFactory.getConnection();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM import_receipts ORDER BY id")) {
            while (rows.next()) {
                var created = rows.getTimestamp("created_at");
                snapshots.add(new ImportReceiptSnapshot(rows.getLong("id"), rows.getString("supplier_name"),
                        rows.getBigDecimal("total_cost"), nullableLong(rows, "created_by_user_id"),
                        created == null ? null : created.toLocalDateTime()));
            }
            return snapshots;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot export import receipts", e);
        }
    }

    public List<ImportItemSnapshot> findAllItems() {
        List<ImportItemSnapshot> snapshots = new ArrayList<>();
        try (var connection = ConnectionFactory.getConnection();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM import_items ORDER BY id")) {
            while (rows.next()) {
                var expiry = rows.getDate("expiry_date");
                snapshots.add(new ImportItemSnapshot(rows.getLong("id"), rows.getLong("import_receipt_id"),
                        rows.getLong("product_id"), rows.getInt("quantity"), rows.getBigDecimal("unit_cost"),
                        rows.getBigDecimal("subtotal"), expiry == null ? null : expiry.toLocalDate()));
            }
            return snapshots;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot export import items and expiry dates", e);
        }
    }

    private Long nullableLong(ResultSet rows, String column) throws SQLException {
        long value = rows.getLong(column);
        return rows.wasNull() ? null : value;
    }
}

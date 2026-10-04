package com.storemanager.core.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Allows old shift rows to coexist with the single-store schema without deleting history. */
public final class LegacySchemaCompatibility {
    private LegacySchemaCompatibility() { }

    public static void allowUnassignedShiftRules(Connection connection) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), null, "work_shift_rules", "branch_id")) {
            if (!columns.next() || columns.getInt("NULLABLE") != java.sql.DatabaseMetaData.columnNoNulls) return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE work_shift_rules MODIFY branch_id BIGINT NULL");
        }
    }
}

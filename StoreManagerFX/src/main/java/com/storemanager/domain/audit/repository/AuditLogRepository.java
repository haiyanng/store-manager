package com.storemanager.domain.audit.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.audit.model.AuditLog;
import com.storemanager.domain.audit.model.AuditLogFilter;
import com.storemanager.domain.audit.model.AuditLogViewDto;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {

    public AuditLogRepository() {

        initializeTables();
    }

    public boolean save(
            AuditLog log
    ) {

        if (log == null) {
            throw new RuntimeException("Audit log is required");
        }

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO audit_logs (
                                    user_id,
                                    actor_username,
                                    module,
                                    action,
                                    entity_type,
                                    entity_id,
                                    success,
                                    reason,
                                    details_json,
                                    details,
                                    created_at
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                                """
                        )
        ) {

            statement.setObject(1, log.getUserId());
            statement.setString(2, log.getActorUsername());
            statement.setString(3, log.getModule());
            statement.setString(4, log.getAction());
            statement.setString(5, log.getEntityType());
            statement.setObject(6, log.getEntityId());
            statement.setObject(7, log.getSuccess());
            statement.setString(8, log.getReason());
            statement.setString(9, log.getDetailsJson());
            statement.setString(10, log.getDetails());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }

    public List<AuditLogViewDto> findAll(
            AuditLogFilter filter
    ) {

        List<AuditLogViewDto> logs =
                new ArrayList<>();

        StringBuilder sql =
                new StringBuilder(
                        """
                        SELECT
                            audit_logs.id AS id,
                            audit_logs.user_id AS user_id,
                            CASE
                                WHEN audit_logs.user_id IS NULL THEN 'System'
                                WHEN users.id IS NULL THEN CONCAT('Deleted User #', audit_logs.user_id)
                                ELSE users.username
                            END AS username,
                            audit_logs.actor_username AS actor_username,
                            audit_logs.module AS module,
                            audit_logs.action AS action,
                            audit_logs.entity_type AS entity_type,
                            audit_logs.entity_id AS entity_id,
                            audit_logs.success AS success,
                            audit_logs.reason AS reason,
                            audit_logs.details_json AS details_json,
                            audit_logs.details AS details,
                            audit_logs.created_at AS created_at
                        FROM audit_logs
                        LEFT JOIN users ON audit_logs.user_id = users.id
                        WHERE 1 = 1
                        """
                );

        List<Object> parameters =
                new ArrayList<>();

        if (filter != null) {
            if (filter.getAction() != null
                    && !filter.getAction().trim().isEmpty()) {
                sql.append(" AND action = ?");
                parameters.add(filter.getAction().trim());
            }

            if (filter.getEntityType() != null
                    && !filter.getEntityType().trim().isEmpty()) {
                sql.append(" AND entity_type = ?");
                parameters.add(filter.getEntityType().trim());
            }

            if (filter.getModule() != null
                    && !filter.getModule().trim().isEmpty()) {
                sql.append(" AND module = ?");
                parameters.add(filter.getModule().trim());
            }

            if (filter.getSuccess() != null) {
                sql.append(" AND success = ?");
                parameters.add(filter.getSuccess());
            }

            if (filter.getUserId() != null) {
                sql.append(" AND user_id = ?");
                parameters.add(filter.getUserId());
            }

            if (filter.getFromDate() != null) {
                sql.append(" AND DATE(created_at) >= ?");
                parameters.add(filter.getFromDate());
            }

            if (filter.getToDate() != null) {
                sql.append(" AND DATE(created_at) <= ?");
                parameters.add(filter.getToDate());
            }
        }

        sql.append(" ORDER BY created_at DESC, id DESC");

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {
                    logs.add(
                            mapAuditLog(resultSet)
                    );
                }
            }

            return logs;

        } catch (Exception e) {
            throw new IllegalStateException("Unable to load records. Check the database connection and try again.", e);
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
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id BIGINT,
                        actor_username VARCHAR(100),
                        module VARCHAR(80),
                        action VARCHAR(80) NOT NULL,
                        entity_type VARCHAR(120) NOT NULL,
                        entity_id BIGINT,
                        success BOOLEAN NOT NULL DEFAULT TRUE,
                        reason VARCHAR(500),
                        details_json TEXT,
                        details TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

            addColumnIfMissing(connection, "actor_username", "ALTER TABLE audit_logs ADD COLUMN actor_username VARCHAR(100)");
            addColumnIfMissing(connection, "module", "ALTER TABLE audit_logs ADD COLUMN module VARCHAR(80)");
            addColumnIfMissing(connection, "success", "ALTER TABLE audit_logs ADD COLUMN success BOOLEAN NOT NULL DEFAULT TRUE");
            addColumnIfMissing(connection, "reason", "ALTER TABLE audit_logs ADD COLUMN reason VARCHAR(500)");
            addColumnIfMissing(connection, "details_json", "ALTER TABLE audit_logs ADD COLUMN details_json TEXT");

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException(
                    "Audit log table initialization failed",
                    e
            );
        }
    }

    private AuditLogViewDto mapAuditLog(
            ResultSet resultSet
    ) throws Exception {

        AuditLogViewDto log =
                new AuditLogViewDto();

        log.setId(resultSet.getLong("id"));
        log.setUserId((Long) resultSet.getObject("user_id"));
        log.setUsername(resultSet.getString("username"));
        log.setActorUsername(resultSet.getString("actor_username"));
        log.setModule(resultSet.getString("module"));
        log.setAction(resultSet.getString("action"));
        log.setEntityType(resultSet.getString("entity_type"));
        log.setEntityId((Long) resultSet.getObject("entity_id"));
        log.setSuccess((Boolean) resultSet.getObject("success"));
        log.setReason(resultSet.getString("reason"));
        log.setDetailsJson(resultSet.getString("details_json"));
        log.setDetails(resultSet.getString("details"));

        Timestamp createdAt =
                resultSet.getTimestamp("created_at");

        if (createdAt != null) {
            log.setCreatedAt(createdAt.toLocalDateTime());
        }

        return log;
    }

    private void addColumnIfMissing(
            Connection connection,
            String column,
            String alterSql
    ) throws Exception {

        DatabaseMetaData metaData =
                connection.getMetaData();

        try (
                ResultSet columns =
                        metaData.getColumns(
                                null,
                                null,
                                "audit_logs",
                                column
                        )
        ) {
            if (columns.next()) {
                return;
            }
        }

        try (
                Statement statement =
                        connection.createStatement()
        ) {
            statement.execute(alterSql);
        }
    }
}

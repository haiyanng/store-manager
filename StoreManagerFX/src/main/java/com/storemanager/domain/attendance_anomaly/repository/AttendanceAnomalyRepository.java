package com.storemanager.domain.attendance_anomaly.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomaly;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyFilter;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalySeverity;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyStatus;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceAnomalyRepository {

    public AttendanceAnomalyRepository() {

        initializeTables();
    }

    public AttendanceAnomaly findById(
            Long id
    ) {

        if (id == null) {
            return null;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        SELECT *
                        FROM attendance_anomalies
                        WHERE id = ?
                        LIMIT 1
                        """
                )
        ) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAnomaly(resultSet);
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public AttendanceAnomaly findByAttendanceSessionIdAndType(
            Long attendanceSessionId,
            AttendanceAnomalyType type
    ) {

        if (attendanceSessionId == null || type == null) {
            return null;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        SELECT *
                        FROM attendance_anomalies
                        WHERE attendance_session_id = ?
                          AND type = ?
                        LIMIT 1
                        """
                )
        ) {

            statement.setLong(1, attendanceSessionId);
            statement.setString(2, type.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAnomaly(resultSet);
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<AttendanceAnomaly> findAll(
            AttendanceAnomalyFilter filter
    ) {

        List<AttendanceAnomaly> anomalies = new ArrayList<>();

        StringBuilder sql =
                new StringBuilder(
                        """
                        SELECT *
                        FROM attendance_anomalies
                        WHERE 1 = 1
                        """
                );

        List<Object> parameters = new ArrayList<>();

        if (filter != null) {
            if (filter.getStatus() != null) {
                sql.append(" AND status = ?");
                parameters.add(filter.getStatus().name());
            }

            if (filter.getType() != null) {
                sql.append(" AND type = ?");
                parameters.add(filter.getType().name());
            }

            if (filter.getSeverity() != null) {
                sql.append(" AND severity = ?");
                parameters.add(filter.getSeverity().name());
            }

            if (filter.getEmployeeId() != null) {
                sql.append(" AND employee_id = ?");
                parameters.add(filter.getEmployeeId());
            }

            if (filter.getBranchId() != null) {
                sql.append(" AND branch_id = ?");
                parameters.add(filter.getBranchId());
            } else if (filter.getBranchIds() != null
                    && !filter.getBranchIds().isEmpty()) {
                sql.append(" AND branch_id IN (");
                for (int i = 0; i < filter.getBranchIds().size(); i++) {
                    if (i > 0) {
                        sql.append(", ");
                    }
                    sql.append("?");
                    parameters.add(filter.getBranchIds().get(i));
                }
                sql.append(")");
            }

            if (filter.getFromDate() != null) {
                sql.append(" AND DATE(created_at) >= ?");
                parameters.add(Date.valueOf(filter.getFromDate()));
            }

            if (filter.getToDate() != null) {
                sql.append(" AND DATE(created_at) <= ?");
                parameters.add(Date.valueOf(filter.getToDate()));
            }
        }

        sql.append(" ORDER BY created_at DESC, id DESC");

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql.toString()
                )
        ) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    anomalies.add(mapAnomaly(resultSet));
                }
            }

            return anomalies;

        } catch (Exception e) {
            throw new IllegalStateException("Unable to load records. Check the database connection and try again.", e);
        }
    }

    public boolean save(
            AttendanceAnomaly anomaly
    ) {

        if (anomaly == null) {
            throw new RuntimeException("Attendance anomaly is required");
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO attendance_anomalies (
                            attendance_session_id,
                            employee_id,
                            branch_id,
                            type,
                            severity,
                            message,
                            status,
                            employee_notified,
                            manager_reported,
                            manager_report_text,
                            resolved_by_user_id,
                            resolved_at,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            statement.setObject(1, anomaly.getAttendanceSessionId());
            statement.setObject(2, anomaly.getEmployeeId());
            statement.setObject(3, anomaly.getBranchId());
            statement.setString(4, anomaly.getType().name());
            statement.setString(5, anomaly.getSeverity().name());
            statement.setString(6, anomaly.getMessage());
            statement.setString(7, anomaly.getStatus().name());
            statement.setBoolean(8, anomaly.isEmployeeNotified());
            statement.setBoolean(9, anomaly.isManagerReported());
            statement.setString(10, anomaly.getManagerReportText());
            statement.setObject(11, anomaly.getResolvedByUserId());
            statement.setTimestamp(
                    12,
                    anomaly.getResolvedAt() == null
                            ? null
                            : Timestamp.valueOf(anomaly.getResolvedAt())
            );

            boolean saved = statement.executeUpdate() > 0;

            if (saved) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        anomaly.setId(generatedKeys.getLong(1));
                    }
                }
            }

            return saved;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStatus(
            Long anomalyId,
            AttendanceAnomalyStatus status,
            Long resolvedByUserId,
            LocalDateTime resolvedAt
    ) {

        if (anomalyId == null || status == null) {
            return false;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        UPDATE attendance_anomalies
                        SET status = ?,
                            resolved_by_user_id = ?,
                            resolved_at = ?
                        WHERE id = ?
                        """
                )
        ) {

            statement.setString(1, status.name());
            statement.setObject(2, resolvedByUserId);
            statement.setTimestamp(
                    3,
                    resolvedAt == null ? null : Timestamp.valueOf(resolvedAt)
            );
            statement.setLong(4, anomalyId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateManagerReport(
            Long anomalyId,
            boolean managerReported,
            String managerReportText
    ) {

        if (anomalyId == null) {
            return false;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        UPDATE attendance_anomalies
                        SET manager_reported = ?,
                            manager_report_text = ?
                        WHERE id = ?
                        """
                )
        ) {

            statement.setBoolean(1, managerReported);
            statement.setString(2, managerReportText);
            statement.setLong(3, anomalyId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateEmployeeNotified(
            Long anomalyId,
            boolean employeeNotified
    ) {

        if (anomalyId == null) {
            return false;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        UPDATE attendance_anomalies
                        SET employee_notified = ?
                        WHERE id = ?
                        """
                )
        ) {

            statement.setBoolean(1, employeeNotified);
            statement.setLong(2, anomalyId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void initializeTables() {

        try (
                Connection connection = ConnectionFactory.getConnection();
                Statement statement = connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS attendance_anomalies (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        attendance_session_id BIGINT NOT NULL,
                        employee_id BIGINT NOT NULL,
                        branch_id BIGINT NULL,
                        type VARCHAR(60) NOT NULL,
                        severity VARCHAR(20) NOT NULL,
                        message TEXT NOT NULL,
                        status VARCHAR(40) NOT NULL,
                        employee_notified BOOLEAN NOT NULL DEFAULT FALSE,
                        manager_reported BOOLEAN NOT NULL DEFAULT FALSE,
                        manager_report_text TEXT NULL,
                        resolved_by_user_id BIGINT NULL,
                        resolved_at TIMESTAMP NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uq_attendance_anomaly_session_type (
                            attendance_session_id,
                            type
                        )
                    )
                    """
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Attendance anomaly table initialization failed",
                    e
            );
        }
    }

    private AttendanceAnomaly mapAnomaly(
            ResultSet resultSet
    ) throws Exception {

        AttendanceAnomaly anomaly = new AttendanceAnomaly();

        anomaly.setId(resultSet.getLong("id"));
        anomaly.setAttendanceSessionId(resultSet.getLong("attendance_session_id"));
        anomaly.setEmployeeId(resultSet.getLong("employee_id"));

        long branchId = resultSet.getLong("branch_id");
        anomaly.setBranchId(resultSet.wasNull() ? null : branchId);

        anomaly.setType(
                AttendanceAnomalyType.valueOf(resultSet.getString("type"))
        );
        anomaly.setSeverity(
                AttendanceAnomalySeverity.valueOf(resultSet.getString("severity"))
        );
        anomaly.setMessage(resultSet.getString("message"));
        anomaly.setStatus(
                AttendanceAnomalyStatus.valueOf(resultSet.getString("status"))
        );
        anomaly.setEmployeeNotified(resultSet.getBoolean("employee_notified"));
        anomaly.setManagerReported(resultSet.getBoolean("manager_reported"));
        anomaly.setManagerReportText(resultSet.getString("manager_report_text"));

        long resolvedByUserId = resultSet.getLong("resolved_by_user_id");
        anomaly.setResolvedByUserId(
                resultSet.wasNull() ? null : resolvedByUserId
        );

        Timestamp resolvedAt = resultSet.getTimestamp("resolved_at");
        if (resolvedAt != null) {
            anomaly.setResolvedAt(resolvedAt.toLocalDateTime());
        }

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            anomaly.setCreatedAt(createdAt.toLocalDateTime());
        }

        return anomaly;
    }
}

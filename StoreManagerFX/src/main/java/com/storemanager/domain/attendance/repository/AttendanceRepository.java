package com.storemanager.domain.attendance.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.util.TimeFormatUtil;
import com.storemanager.domain.attendance.model.AttendanceMonthlyTotal;
import com.storemanager.domain.attendance.model.AttendanceSession;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceRepository {

    public AttendanceRepository() {

        initializeTable();
    }

    public List<AttendanceSession> findAllSessions() {

        List<AttendanceSession> sessions =
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
                                FROM attendance_sessions
                                ORDER BY check_in_time DESC, id DESC
                                """
                        )
        ) {

            while (resultSet.next()) {
                sessions.add(
                        mapSession(resultSet)
                );
            }

            return sessions;

        } catch (Exception e) {

            e.printStackTrace();

            return sessions;
        }
    }

    public AttendanceSession findSessionById(
            Long sessionId
    ) {

        if (sessionId == null) {
            return null;
        }

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM attendance_sessions
                                WHERE id = ?
                                LIMIT 1
                                """
                        )
        ) {

            statement.setLong(1, sessionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSession(resultSet);
                }
            }

            return null;

        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }
    }

    public List<AttendanceMonthlyTotal> findMonthlyTotals(
            LocalDate month
    ) {

        List<AttendanceMonthlyTotal> totals =
                new ArrayList<>();

        LocalDate startDate =
                month.withDayOfMonth(1);

        LocalDate endDate =
                startDate.plusMonths(1);

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT employee_id,
                                       DATE_FORMAT(check_in_time, '%Y-%m') AS month_value,
                                       COALESCE(SUM(worked_hours), 0) AS total_worked_hours
                                FROM attendance_sessions
                                WHERE check_in_time >= ?
                                  AND check_in_time < ?
                                  AND check_out_time IS NOT NULL
                                GROUP BY employee_id, month_value
                                ORDER BY employee_id
                                """
                        )
        ) {

            statement.setTimestamp(
                    1,
                    Timestamp.valueOf(startDate.atStartOfDay())
            );

            statement.setTimestamp(
                    2,
                    Timestamp.valueOf(endDate.atStartOfDay())
            );

            ResultSet resultSet =
                    statement.executeQuery();

            while (resultSet.next()) {
                AttendanceMonthlyTotal total =
                        new AttendanceMonthlyTotal();

                total.setEmployeeId(
                        resultSet.getLong("employee_id")
                );
                total.setMonth(
                        resultSet.getString("month_value")
                );
                total.setTotalWorkedHours(
                        resultSet.getBigDecimal("total_worked_hours")
                );

                totals.add(total);
            }

            return totals;

        } catch (Exception e) {

            e.printStackTrace();

            return totals;
        }
    }

    public AttendanceSession findOpenSessionByEmployeeId(
            Long employeeId
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM attendance_sessions
                                WHERE employee_id = ?
                                  AND check_out_time IS NULL
                                ORDER BY check_in_time DESC, id DESC
                                LIMIT 1
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    employeeId
                );

            ResultSet resultSet =
                    statement.executeQuery();

            if (resultSet.next()) {
                return mapSession(resultSet);
            }

            return null;

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    public boolean checkIn(
            AttendanceSession session
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO attendance_sessions (
                                    employee_id,
                                    branch_id,
                                    check_in_time,
                                    created_by_user_id
                                )
                                VALUES (?, ?, ?, ?)
                                """,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setLong(
                    1,
                    session.getEmployeeId()
            );
            statement.setLong(
                    2,
                    session.getBranchId()
            );
            statement.setTimestamp(
                    3,
                    Timestamp.valueOf(
                            TimeFormatUtil.truncateToSeconds(
                                    session.getCheckInTime()
                            )
                    )
            );

            if (session.getCreatedByUserId() == null) {
                statement.setObject(
                        4,
                        null
                );
            } else {
                statement.setLong(
                        4,
                        session.getCreatedByUserId()
                );
            }

            boolean saved = statement.executeUpdate() > 0;

            if (saved) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        session.setId(generatedKeys.getLong(1));
                    }
                }
            }

            return saved;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean checkOut(
            AttendanceSession session
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE attendance_sessions
                                SET check_out_time = ?,
                                    worked_hours = ?
                                WHERE id = ?
                                """
                        )
        ) {

            statement.setTimestamp(
                    1,
                    Timestamp.valueOf(
                            TimeFormatUtil.truncateToSeconds(
                                    session.getCheckOutTime()
                            )
                    )
            );
            statement.setBigDecimal(
                    2,
                    session.getWorkedHours()
            );
            statement.setLong(
                    3,
                    session.getId()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean updateSession(
            AttendanceSession session
    ) {

        if (session == null || session.getId() == null) {
            return false;
        }

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE attendance_sessions
                                SET employee_id = ?,
                                    branch_id = ?,
                                    check_in_time = ?,
                                    check_out_time = ?,
                                    worked_hours = ?
                                WHERE id = ?
                                """
                        )
        ) {

            statement.setLong(1, session.getEmployeeId());

            if (session.getBranchId() == null) {
                statement.setObject(2, null);
            } else {
                statement.setLong(2, session.getBranchId());
            }

            statement.setTimestamp(
                    3,
                    session.getCheckInTime() == null
                            ? null
                            : Timestamp.valueOf(
                                    TimeFormatUtil.truncateToSeconds(
                                            session.getCheckInTime()
                                    )
                            )
            );
            statement.setTimestamp(
                    4,
                    session.getCheckOutTime() == null
                            ? null
                            : Timestamp.valueOf(
                                    TimeFormatUtil.truncateToSeconds(
                                            session.getCheckOutTime()
                                    )
                            )
            );
            statement.setBigDecimal(5, session.getWorkedHours());
            statement.setLong(6, session.getId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }

    public List<AttendanceSession> findOpenSessions() {

        List<AttendanceSession> sessions =
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
                                FROM attendance_sessions
                                WHERE check_out_time IS NULL
                                ORDER BY check_in_time DESC, id DESC
                                """
                        )
        ) {

            while (resultSet.next()) {
                sessions.add(
                        mapSession(resultSet)
                );
            }

            return sessions;

        } catch (Exception e) {

            e.printStackTrace();

            return sessions;
        }
    }

    private void initializeTable() {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS attendance_sessions (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        employee_id BIGINT NOT NULL,
                        branch_id BIGINT NULL,
                        check_in_time TIMESTAMP NOT NULL,
                        check_out_time TIMESTAMP NULL,
                        worked_hours DECIMAL(10, 2) NULL,
                        created_by_user_id BIGINT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

            addBranchIdColumnIfMissing(connection);

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Attendance table initialization failed",
                    e
            );
        }
    }

    private void addBranchIdColumnIfMissing(
            Connection connection
    ) throws Exception {

        DatabaseMetaData metaData =
                connection.getMetaData();

        try (
                ResultSet columns =
                        metaData.getColumns(
                                null,
                                null,
                                "attendance_sessions",
                                "branch_id"
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

            statement.execute(
                    """
                    ALTER TABLE attendance_sessions
                    ADD COLUMN branch_id BIGINT NULL
                    """
            );
        }
    }

    private AttendanceSession mapSession(
            ResultSet resultSet
    ) throws Exception {

        AttendanceSession session =
                new AttendanceSession();

        session.setId(resultSet.getLong("id"));
        session.setEmployeeId(resultSet.getLong("employee_id"));
        long branchId =
                resultSet.getLong("branch_id");

        if (resultSet.wasNull()) {
            session.setBranchId(null);
        } else {
            session.setBranchId(branchId);
        }
        session.setCheckInTime(
                toLocalDateTime(
                        resultSet.getTimestamp("check_in_time")
                )
        );
        session.setCheckOutTime(
                toLocalDateTime(
                        resultSet.getTimestamp("check_out_time")
                )
        );

        BigDecimal workedHours =
                resultSet.getBigDecimal("worked_hours");

        session.setWorkedHours(workedHours);

        long createdByUserId =
                resultSet.getLong("created_by_user_id");

        if (resultSet.wasNull()) {
            session.setCreatedByUserId(null);
        } else {
            session.setCreatedByUserId(createdByUserId);
        }

        session.setCreatedAt(
                toLocalDateTime(
                        resultSet.getTimestamp("created_at")
                )
        );

        return session;
    }

    private LocalDateTime toLocalDateTime(
            Timestamp timestamp
    ) {

        if (timestamp == null) {
            return null;
        }

        return TimeFormatUtil.truncateToSeconds(
                timestamp.toLocalDateTime()
        );
    }
}

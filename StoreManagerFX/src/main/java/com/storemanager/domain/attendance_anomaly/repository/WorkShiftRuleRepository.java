package com.storemanager.domain.attendance_anomaly.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.attendance_anomaly.model.WorkShiftRule;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkShiftRuleRepository {

    public WorkShiftRuleRepository() {

        initializeTables();
    }

    public List<WorkShiftRule> findAll() {

        List<WorkShiftRule> rules = new ArrayList<>();

        try (
                Connection connection = ConnectionFactory.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        """
                        SELECT *
                        FROM work_shift_rules
                        ORDER BY active DESC, branch_id, id DESC
                        """
                )
        ) {

            while (resultSet.next()) {
                rules.add(mapRule(resultSet));
            }

            return rules;

        } catch (Exception e) {
            e.printStackTrace();
            return rules;
        }
    }

    public List<WorkShiftRule> findByBranchId(
            Long branchId
    ) {

        List<WorkShiftRule> rules = new ArrayList<>();

        if (branchId == null) {
            return rules;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        SELECT *
                        FROM work_shift_rules
                        WHERE branch_id = ?
                        ORDER BY active DESC, id DESC
                        """
                )
        ) {

            statement.setLong(1, branchId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rules.add(mapRule(resultSet));
                }
            }

            return rules;

        } catch (Exception e) {
            e.printStackTrace();
            return rules;
        }
    }

    public WorkShiftRule findActiveByBranchId(
            Long branchId
    ) {

        if (branchId == null) {
            return null;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        SELECT *
                        FROM work_shift_rules
                        WHERE branch_id = ?
                          AND active = TRUE
                        ORDER BY id DESC
                        LIMIT 1
                        """
                )
        ) {

            statement.setLong(1, branchId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRule(resultSet);
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean save(
            WorkShiftRule rule
    ) {

        if (rule == null) {
            throw new RuntimeException("Work shift rule is required");
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO work_shift_rules (
                            branch_id,
                            shift_name,
                            start_time,
                            end_time,
                            late_tolerance_minutes,
                            early_tolerance_minutes,
                            max_work_hours,
                            active,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            fillStatement(statement, rule);

            boolean saved = statement.executeUpdate() > 0;

            if (saved) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        rule.setId(generatedKeys.getLong(1));
                    }
                }
            }

            return saved;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(
            WorkShiftRule rule
    ) {

        if (rule == null || rule.getId() == null) {
            return false;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        UPDATE work_shift_rules
                        SET branch_id = ?,
                            shift_name = ?,
                            start_time = ?,
                            end_time = ?,
                            late_tolerance_minutes = ?,
                            early_tolerance_minutes = ?,
                            max_work_hours = ?,
                            active = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """
                )
        ) {

            fillStatement(statement, rule);
            statement.setLong(9, rule.getId());

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
                    CREATE TABLE IF NOT EXISTS work_shift_rules (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        branch_id BIGINT NOT NULL,
                        shift_name VARCHAR(120) NOT NULL,
                        start_time TIME NOT NULL,
                        end_time TIME NOT NULL,
                        late_tolerance_minutes INT NOT NULL DEFAULT 0,
                        early_tolerance_minutes INT NOT NULL DEFAULT 0,
                        max_work_hours DECIMAL(10, 2) NOT NULL DEFAULT 8.00,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP
                    )
                    """
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Work shift rule table initialization failed",
                    e
            );
        }
    }

    private void fillStatement(
            PreparedStatement statement,
            WorkShiftRule rule
    ) throws Exception {

        statement.setObject(1, rule.getBranchId());
        statement.setString(2, rule.getShiftName());
        statement.setTime(3, Time.valueOf(rule.getStartTime()));
        statement.setTime(4, Time.valueOf(rule.getEndTime()));
        statement.setObject(5, rule.getLateToleranceMinutes());
        statement.setObject(6, rule.getEarlyToleranceMinutes());
        statement.setBigDecimal(
                7,
                rule.getMaxWorkHours() == null
                        ? BigDecimal.ZERO
                        : rule.getMaxWorkHours()
        );
        statement.setBoolean(8, rule.isActive());
    }

    private WorkShiftRule mapRule(
            ResultSet resultSet
    ) throws Exception {

        WorkShiftRule rule = new WorkShiftRule();

        rule.setId(resultSet.getLong("id"));
        rule.setBranchId(resultSet.getLong("branch_id"));
        rule.setShiftName(resultSet.getString("shift_name"));
        rule.setStartTime(resultSet.getTime("start_time").toLocalTime());
        rule.setEndTime(resultSet.getTime("end_time").toLocalTime());
        rule.setLateToleranceMinutes(
                resultSet.getInt("late_tolerance_minutes")
        );
        rule.setEarlyToleranceMinutes(
                resultSet.getInt("early_tolerance_minutes")
        );
        rule.setMaxWorkHours(resultSet.getBigDecimal("max_work_hours"));
        rule.setActive(resultSet.getBoolean("active"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            rule.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            rule.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return rule;
    }
}

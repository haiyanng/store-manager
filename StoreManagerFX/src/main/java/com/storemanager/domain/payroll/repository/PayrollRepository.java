package com.storemanager.domain.payroll.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.payroll.model.EmployeeSalaryConfig;
import com.storemanager.domain.payroll.model.PayrollRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PayrollRepository {

    public PayrollRepository() {

        initializeTables();
    }

    public List<EmployeeSalaryConfig> findAllSalaryConfigs() {

        List<EmployeeSalaryConfig> configs =
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
                                FROM employee_salary_configs
                                ORDER BY employee_id
                                """
                        )
        ) {

            while (resultSet.next()) {
                configs.add(mapSalaryConfig(resultSet));
            }

            return configs;

        } catch (Exception e) {
            e.printStackTrace();
            return configs;
        }
    }

    public EmployeeSalaryConfig findActiveConfigByEmployeeId(
            Long employeeId
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM employee_salary_configs
                                WHERE employee_id = ?
                                  AND active = TRUE
                                LIMIT 1
                                """
                        )
        ) {

            statement.setLong(1, employeeId);

            ResultSet resultSet =
                    statement.executeQuery();

            if (resultSet.next()) {
                return mapSalaryConfig(resultSet);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean saveOrUpdateSalaryConfig(
            EmployeeSalaryConfig config
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO employee_salary_configs (
                                    employee_id,
                                    hourly_rate,
                                    active,
                                    updated_at
                                )
                                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                                ON DUPLICATE KEY UPDATE
                                    hourly_rate = VALUES(hourly_rate),
                                    active = VALUES(active),
                                    updated_at = CURRENT_TIMESTAMP
                                """
                        )
        ) {

            statement.setLong(1, config.getEmployeeId());
            statement.setBigDecimal(2, config.getHourlyRate());
            statement.setBoolean(3, config.isActive());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<PayrollRecord> findPayrollRecords() {

        List<PayrollRecord> records =
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
                                FROM payroll_records
                                ORDER BY year DESC, month DESC, employee_id
                                """
                        )
        ) {

            while (resultSet.next()) {
                records.add(mapPayrollRecord(resultSet));
            }

            return records;

        } catch (Exception e) {
            e.printStackTrace();
            return records;
        }
    }

    public List<PayrollRecord> findPayrollRecordsByEmployeeId(
            Long employeeId
    ) {

        List<PayrollRecord> records = new ArrayList<>();

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM payroll_records
                                WHERE employee_id = ?
                                ORDER BY year DESC, month DESC
                                """
                        )
        ) {

            statement.setLong(1, employeeId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                records.add(mapPayrollRecord(resultSet));
            }

            return records;

        } catch (Exception e) {
            e.printStackTrace();
            return records;
        }
    }

    public BigDecimal findTotalPayrollCost() {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                Statement statement =
                        connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                """
                                SELECT COALESCE(SUM(total_salary), 0) AS total_payroll_cost
                                FROM payroll_records
                                """
                        )
        ) {

            if (resultSet.next()) {
                return resultSet.getBigDecimal("total_payroll_cost");
            }

            return BigDecimal.ZERO;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Cannot load total payroll cost",
                    e
            );
        }
    }

    public BigDecimal findPayrollCostForPeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT COALESCE(SUM(total_salary), 0) AS total_payroll_cost
                                FROM payroll_records
                                WHERE generated_at >= ?
                                  AND generated_at < ?
                                """
                        )
        ) {

            statement.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(endDate.atStartOfDay()));

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBigDecimal("total_payroll_cost");
            }

            return BigDecimal.ZERO;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Cannot load payroll cost for period",
                    e
            );
        }
    }

    public long countPayrollRecordsForPeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT COUNT(*) AS payroll_count
                                FROM payroll_records
                                WHERE generated_at >= ?
                                  AND generated_at < ?
                                """
                        )
        ) {

            statement.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(endDate.atStartOfDay()));

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getLong("payroll_count");
            }

            return 0L;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Cannot load payroll record count for period",
                    e
            );
        }
    }

    public boolean savePayrollRecord(
            PayrollRecord record
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO payroll_records (
                                    employee_id,
                                    month,
                                    year,
                                    total_hours,
                                    hourly_rate_snapshot,
                                    total_salary,
                                    generated_at,
                                    generated_by_user_id
                                )
                                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
                                ON DUPLICATE KEY UPDATE
                                    total_hours = VALUES(total_hours),
                                    hourly_rate_snapshot = VALUES(hourly_rate_snapshot),
                                    total_salary = VALUES(total_salary),
                                    generated_at = CURRENT_TIMESTAMP,
                                    generated_by_user_id = VALUES(generated_by_user_id)
                                """
                        )
        ) {

            statement.setLong(1, record.getEmployeeId());
            statement.setInt(2, record.getMonth());
            statement.setInt(3, record.getYear());
            statement.setBigDecimal(4, record.getTotalHours());
            statement.setBigDecimal(5, record.getHourlyRateSnapshot());
            statement.setBigDecimal(6, record.getTotalSalary());

            if (record.getGeneratedByUserId() == null) {
                statement.setObject(7, null);
            } else {
                statement.setLong(7, record.getGeneratedByUserId());
            }

            return statement.executeUpdate() > 0;

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
                    CREATE TABLE IF NOT EXISTS employee_salary_configs (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        employee_id BIGINT NOT NULL UNIQUE,
                        hourly_rate DECIMAL(18, 2) NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP
                    )
                    """
            );

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS payroll_records (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        employee_id BIGINT NOT NULL,
                        month INT NOT NULL,
                        year INT NOT NULL,
                        total_hours DECIMAL(10, 2) NOT NULL DEFAULT 0,
                        hourly_rate_snapshot DECIMAL(18, 2) NOT NULL,
                        total_salary DECIMAL(18, 2) NOT NULL,
                        generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        generated_by_user_id BIGINT,
                        UNIQUE KEY uk_payroll_employee_month_year (
                            employee_id,
                            month,
                            year
                        )
                    )
                    """
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Payroll table initialization failed",
                    e
            );
        }
    }

    private EmployeeSalaryConfig mapSalaryConfig(
            ResultSet resultSet
    ) throws Exception {

        EmployeeSalaryConfig config =
                new EmployeeSalaryConfig();

        config.setId(resultSet.getLong("id"));
        config.setEmployeeId(resultSet.getLong("employee_id"));
        config.setHourlyRate(resultSet.getBigDecimal("hourly_rate"));
        config.setActive(resultSet.getBoolean("active"));

        Timestamp updatedAt =
                resultSet.getTimestamp("updated_at");

        if (updatedAt != null) {
            config.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return config;
    }

    private PayrollRecord mapPayrollRecord(
            ResultSet resultSet
    ) throws Exception {

        PayrollRecord record =
                new PayrollRecord();

        record.setId(resultSet.getLong("id"));
        record.setEmployeeId(resultSet.getLong("employee_id"));
        record.setMonth(resultSet.getInt("month"));
        record.setYear(resultSet.getInt("year"));
        record.setTotalHours(resultSet.getBigDecimal("total_hours"));
        record.setHourlyRateSnapshot(
                resultSet.getBigDecimal("hourly_rate_snapshot")
        );
        record.setTotalSalary(resultSet.getBigDecimal("total_salary"));

        Timestamp generatedAt =
                resultSet.getTimestamp("generated_at");

        if (generatedAt != null) {
            record.setGeneratedAt(generatedAt.toLocalDateTime());
        }

        long generatedByUserId =
                resultSet.getLong("generated_by_user_id");

        if (resultSet.wasNull()) {
            record.setGeneratedByUserId(null);
        } else {
            record.setGeneratedByUserId(generatedByUserId);
        }

        return record;
    }
}

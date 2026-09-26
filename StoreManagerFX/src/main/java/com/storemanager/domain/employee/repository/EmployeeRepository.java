package com.storemanager.domain.employee.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.employee.model.Employee;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {

    public EmployeeRepository() {

        initializeTable();
    }

    public List<Employee> findAll() {

        List<Employee> employees =
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
                                FROM employees
                                ORDER BY id
                                """
                        )

        ) {

            while (resultSet.next()) {
                employees.add(
                        mapEmployee(resultSet)
                );
            }

            return employees;

        } catch (Exception e) {

            throw new IllegalStateException("Unable to load employees. Check the database connection and try again.", e);
        }
    }

    public boolean save(
            Employee employee
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO employees (
                                    full_name,
                                    phone,
                                    address,
                                    position,
                                    user_id,
                                    image_path,
                                    active
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?)
                                """, Statement.RETURN_GENERATED_KEYS
                        )

        ) {

            fillStatement(
                    statement,
                    employee
            );

            boolean saved = statement.executeUpdate() > 0;
            if (saved) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) employee.setId(keys.getLong(1));
                }
            }
            return saved;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean update(
            Employee employee
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE employees
                                SET full_name = ?,
                                    phone = ?,
                                    address = ?,
                                    position = ?,
                                    user_id = ?,
                                    image_path = ?,
                                    active = ?
                                WHERE id = ?
                                """
                        )

        ) {

            fillStatement(
                    statement,
                    employee
            );

            statement.setLong(
                    8,
                    employee.getId()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean clearUserLink(
            Long userId
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE employees
                                SET user_id = NULL
                                WHERE user_id = ?
                                """
                        )

        ) {

            statement.setLong(
                    1,
                    userId
            );

            statement.executeUpdate();

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean linkUserToEmployee(Long userId, Long employeeId) {
        try (Connection connection = ConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement clear = connection.prepareStatement("UPDATE employees SET user_id = NULL WHERE user_id = ?");
                 PreparedStatement link = connection.prepareStatement("UPDATE employees SET user_id = ? WHERE id = ? AND (user_id IS NULL OR user_id = ?)")) {
                clear.setLong(1, userId);
                clear.executeUpdate();
                link.setLong(1, userId);
                link.setLong(2, employeeId);
                link.setLong(3, userId);
                if (link.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }
                connection.commit();
                return true;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Employee findByUserId(
            Long userId
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM employees
                                WHERE user_id = ?
                                """
                        )

        ) {

            statement.setLong(
                    1,
                    userId
            );

            ResultSet resultSet =
                    statement.executeQuery();

            if (resultSet.next()) {
                return mapEmployee(resultSet);
            }

            return null;

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    public Employee findById(
            Long id
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM employees
                                WHERE id = ?
                                """
                        )

        ) {

            statement.setLong(
                    1,
                    id
            );

            ResultSet resultSet =
                    statement.executeQuery();

            if (resultSet.next()) {
                return mapEmployee(resultSet);
            }

            return null;

        } catch (Exception e) {

            throw new IllegalStateException("Unable to load employee. Check the database connection and try again.", e);
        }
    }

    public boolean delete(
            Employee employee
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                DELETE FROM employees
                                WHERE id = ?
                                """
                        )

        ) {

            statement.setLong(
                    1,
                    employee.getId()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
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
                    CREATE TABLE IF NOT EXISTS employees (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        full_name VARCHAR(150) NOT NULL,
                        phone VARCHAR(40),
                        address VARCHAR(255),
                        position VARCHAR(100) NOT NULL,
                        user_id BIGINT NULL,
                        image_path VARCHAR(255) NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

            addUserIdColumnIfMissing(connection);
            addImagePathColumnIfMissing(connection);
            addUserIdUniqueIndexIfMissing(connection);

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Employee table initialization failed",
                    e
            );
        }
    }

    private void addImagePathColumnIfMissing(
            Connection connection
    ) throws Exception {

        DatabaseMetaData metaData =
                connection.getMetaData();

        try (
                ResultSet columns =
                        metaData.getColumns(
                                null,
                                null,
                                "employees",
                                "image_path"
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
                    ALTER TABLE employees
                    ADD COLUMN image_path VARCHAR(255) NULL
                    """
            );
        }
    }

    private void addUserIdColumnIfMissing(
            Connection connection
    ) throws Exception {

        DatabaseMetaData metaData =
                connection.getMetaData();

        try (

                ResultSet columns =
                        metaData.getColumns(
                                null,
                                null,
                                "employees",
                                "user_id"
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
                    ALTER TABLE employees
                    ADD COLUMN user_id BIGINT NULL
                    """
            );
        }
    }

    private void addUserIdUniqueIndexIfMissing(
            Connection connection
    ) throws Exception {

        DatabaseMetaData metaData =
                connection.getMetaData();

        try (

                ResultSet indexes =
                        metaData.getIndexInfo(
                                null,
                                null,
                                "employees",
                                true,
                                false
                        )

        ) {

            while (indexes.next()) {
                String indexName =
                        indexes.getString("INDEX_NAME");

                if ("uk_employees_user_id".equals(indexName)) {
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
                    CREATE UNIQUE INDEX uk_employees_user_id
                    ON employees (user_id)
                    """
            );
        }
    }

    private void fillStatement(
            PreparedStatement statement,
            Employee employee
    ) throws Exception {

        statement.setString(
                1,
                employee.getFullName()
        );

        statement.setString(
                2,
                employee.getPhone()
        );

        statement.setString(
                3,
                employee.getAddress()
        );

        statement.setString(
                4,
                employee.getPosition()
        );

        if (employee.getUserId() == null) {
            statement.setObject(
                    5,
                    null
            );
        } else {
            statement.setLong(
                    5,
                    employee.getUserId()
            );
        }

        statement.setString(
                6,
                employee.getImagePath()
        );

        statement.setBoolean(
                7,
                employee.isActive()
        );
    }

    private Employee mapEmployee(
            ResultSet resultSet
    ) throws Exception {

        Employee employee =
                new Employee();

        employee.setId(
                resultSet.getLong("id")
        );

        employee.setFullName(
                resultSet.getString("full_name")
        );

        employee.setPhone(
                resultSet.getString("phone")
        );

        employee.setAddress(
                resultSet.getString("address")
        );

        employee.setPosition(
                resultSet.getString("position")
        );

        long userId =
                resultSet.getLong("user_id");

        if (resultSet.wasNull()) {
            employee.setUserId(null);
        } else {
            employee.setUserId(userId);
        }

        employee.setImagePath(
                resultSet.getString("image_path")
        );

        employee.setActive(
                resultSet.getBoolean("active")
        );

        return employee;
    }
}

package com.storemanager.domain.branch.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.model.EmployeeBranchAssignment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BranchRepository {

    public BranchRepository() {

        initializeTables();
    }

    public List<Branch> findAllBranches() {

        List<Branch> branches = new ArrayList<>();

        try (
                Connection connection = ConnectionFactory.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        """
                        SELECT *
                        FROM branches
                        ORDER BY id
                        """
                )
        ) {

            while (resultSet.next()) {
                branches.add(mapBranch(resultSet));
            }

            return branches;

        } catch (Exception e) {
            e.printStackTrace();
            return branches;
        }
    }

    public List<Branch> findActiveBranches() {

        List<Branch> branches = new ArrayList<>();

        try (
                Connection connection = ConnectionFactory.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        """
                        SELECT *
                        FROM branches
                        WHERE active = TRUE
                        ORDER BY name
                        """
                )
        ) {

            while (resultSet.next()) {
                branches.add(mapBranch(resultSet));
            }

            return branches;

        } catch (Exception e) {
            e.printStackTrace();
            return branches;
        }
    }

    public List<Branch> findActiveBranchesForEmployeeId(
            Long employeeId
    ) {

        List<Branch> branches = new ArrayList<>();

        if (employeeId == null) {
            return branches;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        SELECT b.*
                        FROM branches b
                        INNER JOIN employee_branch_assignments a
                                ON a.branch_id = b.id
                        WHERE a.employee_id = ?
                          AND a.active = TRUE
                          AND b.active = TRUE
                        ORDER BY a.assigned_at DESC, a.id DESC
                        LIMIT 1
                        """
                )
        ) {

            statement.setLong(1, employeeId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    branches.add(mapBranch(resultSet));
                }
            }

            return branches;

        } catch (Exception e) {
            e.printStackTrace();
            return branches;
        }
    }

    public EmployeeBranchAssignment findActiveAssignmentByEmployeeId(
            Long employeeId
    ) {

        if (employeeId == null) {
            return null;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        SELECT *
                        FROM employee_branch_assignments
                        WHERE employee_id = ?
                          AND active = TRUE
                        ORDER BY assigned_at DESC, id DESC
                        LIMIT 1
                        """
                )
        ) {

            statement.setLong(1, employeeId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAssignment(resultSet);
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Branch findBranchById(
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
                        FROM branches
                        WHERE id = ?
                        LIMIT 1
                        """
                )
        ) {

            statement.setLong(1, branchId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBranch(resultSet);
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Branch findFirstActiveBranch() {

        try (
                Connection connection = ConnectionFactory.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        """
                        SELECT *
                        FROM branches
                        WHERE active = TRUE
                        ORDER BY id
                        LIMIT 1
                        """
                )
        ) {

            if (resultSet.next()) {
                return mapBranch(resultSet);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<EmployeeBranchAssignment> findAllAssignments() {

        List<EmployeeBranchAssignment> assignments = new ArrayList<>();

        try (
                Connection connection = ConnectionFactory.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        """
                        SELECT *
                        FROM employee_branch_assignments
                        ORDER BY assigned_at DESC, id DESC
                        """
                )
        ) {

            while (resultSet.next()) {
                assignments.add(mapAssignment(resultSet));
            }

            return assignments;

        } catch (Exception e) {
            e.printStackTrace();
            return assignments;
        }
    }

    public boolean saveBranch(Branch branch) {

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO branches (
                            name,
                            address,
                            active
                        )
                        VALUES (?, ?, ?)
                        """
                )
        ) {

            fillBranchStatement(statement, branch);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateBranch(Branch branch) {

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        UPDATE branches
                        SET name = ?,
                            address = ?,
                            active = ?
                        WHERE id = ?
                        """
                )
        ) {

            fillBranchStatement(statement, branch);
            statement.setLong(4, branch.getId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveAssignment(EmployeeBranchAssignment assignment) {

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO employee_branch_assignments (
                            employee_id,
                            branch_id,
                            active,
                            assigned_by_user_id
                        )
                        VALUES (?, ?, ?, ?)
                        """
                )
        ) {

            statement.setLong(1, assignment.getEmployeeId());
            statement.setLong(2, assignment.getBranchId());
            statement.setBoolean(3, assignment.isActive());

            if (assignment.getAssignedByUserId() == null) {
                statement.setObject(4, null);
            } else {
                statement.setLong(4, assignment.getAssignedByUserId());
            }

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean assignEmployeeToBranch(
            Long employeeId,
            Long branchId,
            Long assignedByUserId
    ) {

        if (employeeId == null || branchId == null) {
            return false;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO employee_branch_assignments (
                            employee_id,
                            branch_id,
                            active,
                            assigned_by_user_id
                        )
                        VALUES (?, ?, TRUE, ?)
                        """
                )
        ) {

            statement.setLong(1, employeeId);
            statement.setLong(2, branchId);

            if (assignedByUserId == null) {
                statement.setObject(3, null);
            } else {
                statement.setLong(3, assignedByUserId);
            }

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int deactivateActiveAssignments(
            Long employeeId
    ) {

        if (employeeId == null) {
            return 0;
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        UPDATE employee_branch_assignments
                        SET active = FALSE
                        WHERE employee_id = ?
                          AND active = TRUE
                        """
                )
        ) {

            statement.setLong(1, employeeId);
            return statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean transferEmployeeToBranch(
            Long employeeId,
            Long branchId,
            Long assignedByUserId
    ) {

        if (employeeId == null || branchId == null) {
            return false;
        }

        try (
                Connection connection = ConnectionFactory.getConnection()
        ) {

            connection.setAutoCommit(false);

            try (
                    PreparedStatement deactivateStatement =
                            connection.prepareStatement(
                                    """
                                    UPDATE employee_branch_assignments
                                    SET active = FALSE
                                    WHERE employee_id = ?
                                      AND active = TRUE
                                    """
                            );
                    PreparedStatement insertStatement =
                            connection.prepareStatement(
                                    """
                                    INSERT INTO employee_branch_assignments (
                                        employee_id,
                                        branch_id,
                                        active,
                                        assigned_by_user_id
                                    )
                                    VALUES (?, ?, TRUE, ?)
                                    """
                            )
            ) {

                deactivateStatement.setLong(1, employeeId);
                deactivateStatement.executeUpdate();

                insertStatement.setLong(1, employeeId);
                insertStatement.setLong(2, branchId);
                if (assignedByUserId == null) {
                    insertStatement.setObject(3, null);
                } else {
                    insertStatement.setLong(3, assignedByUserId);
                }

                boolean saved = insertStatement.executeUpdate() > 0;
                if (saved) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return saved;

            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deactivateAssignment(EmployeeBranchAssignment assignment) {

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        UPDATE employee_branch_assignments
                        SET active = FALSE
                        WHERE id = ?
                        """
                )
        ) {

            statement.setLong(1, assignment.getId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasActiveAssignment(Long employeeId, Long branchId) {

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        SELECT id
                        FROM employee_branch_assignments
                        WHERE employee_id = ?
                          AND branch_id = ?
                          AND active = TRUE
                        LIMIT 1
                        """
                )
        ) {

            statement.setLong(1, employeeId);
            statement.setLong(2, branchId);

            ResultSet resultSet = statement.executeQuery();

            return resultSet.next();

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
                    CREATE TABLE IF NOT EXISTS branches (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(160) NOT NULL,
                        address VARCHAR(255),
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS employee_branch_assignments (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        employee_id BIGINT NOT NULL,
                        branch_id BIGINT NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        assigned_by_user_id BIGINT
                    )
                    """
            );

            normalizeActiveAssignments();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Branch table initialization failed", e);
        }
    }

    private void normalizeActiveAssignments() {

        try {
            List<EmployeeBranchAssignment> activeAssignments =
                    findAllAssignments()
                            .stream()
                            .filter(EmployeeBranchAssignment::isActive)
                            .sorted(
                                    Comparator.comparing(
                                                    EmployeeBranchAssignment::getEmployeeId,
                                                    Comparator.nullsLast(Comparator.naturalOrder())
                                            )
                                            .thenComparing(
                                                    EmployeeBranchAssignment::getAssignedAt,
                                                    Comparator.nullsLast(Comparator.reverseOrder())
                                            )
                                            .thenComparing(
                                                    EmployeeBranchAssignment::getId,
                                                    Comparator.nullsLast(Comparator.reverseOrder())
                                            )
                            )
                            .toList();

            Map<Long, List<EmployeeBranchAssignment>> assignmentsByEmployee =
                    activeAssignments.stream()
                            .collect(
                                    Collectors.groupingBy(
                                            EmployeeBranchAssignment::getEmployeeId,
                                            LinkedHashMap::new,
                                            Collectors.toList()
                                    )
                            );

            for (List<EmployeeBranchAssignment> assignments : assignmentsByEmployee.values()) {
                if (assignments.size() <= 1) {
                    continue;
                }

                for (int i = 1; i < assignments.size(); i++) {
                    EmployeeBranchAssignment assignment = assignments.get(i);
                    if (assignment != null && assignment.getId() != null) {
                        deactivateAssignment(assignment);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "[BRANCH_ASSIGNMENT_CLEANUP] Unable to normalize legacy active assignments"
            );
            e.printStackTrace();
        }
    }

    private void fillBranchStatement(
            PreparedStatement statement,
            Branch branch
    ) throws Exception {

        statement.setString(1, branch.getName());
        statement.setString(2, branch.getAddress());
        statement.setBoolean(3, branch.isActive());
    }

    private Branch mapBranch(ResultSet resultSet) throws Exception {

        Branch branch = new Branch();

        branch.setId(resultSet.getLong("id"));
        branch.setName(resultSet.getString("name"));
        branch.setAddress(resultSet.getString("address"));
        branch.setActive(resultSet.getBoolean("active"));

        return branch;
    }

    private EmployeeBranchAssignment mapAssignment(
            ResultSet resultSet
    ) throws Exception {

        EmployeeBranchAssignment assignment =
                new EmployeeBranchAssignment();

        assignment.setId(resultSet.getLong("id"));
        assignment.setEmployeeId(resultSet.getLong("employee_id"));
        assignment.setBranchId(resultSet.getLong("branch_id"));
        assignment.setActive(resultSet.getBoolean("active"));

        Timestamp assignedAt = resultSet.getTimestamp("assigned_at");

        if (assignedAt != null) {
            assignment.setAssignedAt(assignedAt.toLocalDateTime());
        }

        long assignedByUserId =
                resultSet.getLong("assigned_by_user_id");

        if (resultSet.wasNull()) {
            assignment.setAssignedByUserId(null);
        } else {
            assignment.setAssignedByUserId(assignedByUserId);
        }

        return assignment;
    }
}

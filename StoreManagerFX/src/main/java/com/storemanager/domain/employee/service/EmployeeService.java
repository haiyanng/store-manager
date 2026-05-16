package com.storemanager.domain.employee.service;

import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.repository.EmployeeRepository;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EmployeeService {

    private final EmployeeRepository employeeRepository =
            new EmployeeRepository();

    private final UserRepository userRepository =
            new UserRepository();

    public List<Employee> findAll() {

        return employeeRepository.findAll();
    }

    public boolean create(
            Employee employee
    ) {

        validate(employee);

        return employeeRepository.save(employee);
    }

    public boolean update(
            Employee employee
    ) {

        if (employee.getId() == null) {
            throw new RuntimeException(
                    "Employee is required"
            );
        }

        validate(employee);

        return employeeRepository.update(employee);
    }

    public boolean delete(
            Employee employee
    ) {

        if (employee == null || employee.getId() == null) {
            throw new RuntimeException(
                    "Employee is required"
            );
        }

        return employeeRepository.delete(employee);
    }

    public Map<Long, User> findUsersById() {

        return userRepository
                .findAll()
                .stream()
                .collect(
                        Collectors.toMap(
                                User::getId,
                                user -> user
                        )
                );
    }

    private void validate(
            Employee employee
    ) {

        if (employee == null) {
            throw new RuntimeException(
                    "Employee is required"
            );
        }

        if (employee.getFullName() == null
                || employee.getFullName().trim().isEmpty()) {
            throw new RuntimeException(
                    "Full name is required"
            );
        }

        if (employee.getPosition() == null
                || employee.getPosition().trim().isEmpty()) {
            throw new RuntimeException(
                    "Position is required"
            );
        }

        employee.setFullName(
                employee.getFullName().trim()
        );

        employee.setPhone(
                clean(employee.getPhone())
        );

        employee.setAddress(
                clean(employee.getAddress())
        );

        employee.setPosition(
                employee.getPosition().trim()
        );
    }

    private String clean(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }
}

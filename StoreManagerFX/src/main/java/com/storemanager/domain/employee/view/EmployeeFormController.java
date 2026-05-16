package com.storemanager.domain.employee.view;

import com.storemanager.domain.employee.model.Employee;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class EmployeeFormController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField positionField;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    public void initialize() {

        activeCheckBox.setSelected(true);
    }

    public Employee readEmployee() {

        Employee employee =
                new Employee();

        employee.setFullName(
                fullNameField.getText()
        );

        employee.setPhone(
                phoneField.getText()
        );

        employee.setAddress(
                addressField.getText()
        );

        employee.setPosition(
                positionField.getText()
        );

        employee.setActive(
                activeCheckBox.isSelected()
        );

        return employee;
    }

    public void showEmployee(
            Employee employee
    ) {

        if (employee == null) {
            clear();
            return;
        }

        fullNameField.setText(
                employee.getFullName()
        );

        phoneField.setText(
                employee.getPhone()
        );

        addressField.setText(
                employee.getAddress()
        );

        positionField.setText(
                employee.getPosition()
        );

        activeCheckBox.setSelected(
                employee.isActive()
        );
    }

    public void clear() {

        fullNameField.clear();
        phoneField.clear();
        addressField.clear();
        positionField.clear();
        activeCheckBox.setSelected(true);
    }
}

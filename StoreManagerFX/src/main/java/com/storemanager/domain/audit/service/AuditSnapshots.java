package com.storemanager.domain.audit.service;

import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.user.model.User;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit audit fields: never serialize an entire entity, especially a User. */
public final class AuditSnapshots {
    private AuditSnapshots() { }

    public static Map<String, Object> employee(Employee value) {
        if (value == null) return null;
        return fields("id", value.getId(), "full_name", value.getFullName(),
                "phone", value.getPhone(), "address", value.getAddress(),
                "position", value.getPosition(), "user_id", value.getUserId(),
                "image_path", value.getImagePath(), "active", value.isActive());
    }

    public static Map<String, Object> category(Category value) {
        if (value == null) return null;
        return fields("id", value.getId(), "name", value.getName(),
                "image_path", value.getImagePath(), "active", value.isActive());
    }

    public static Map<String, Object> product(Product value) {
        if (value == null) return null;
        return fields("id", value.getId(), "name", value.getName(), "sku", value.getSku(),
                "barcode", value.getBarcode(), "category_id", value.getCategoryId(),
                "base_price", value.getBasePrice(), "unit", value.getUnit(),
                "image_path", value.getImagePath(), "active", value.isActive());
    }

    public static Map<String, Object> user(User value) {
        if (value == null) return null;
        return fields("id", value.getId(), "username", value.getUsername(),
                "role", value.getRole(), "active", value.isActive());
    }

    public static Map<String, Object> fields(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], values[i + 1]);
        }
        return result;
    }
}

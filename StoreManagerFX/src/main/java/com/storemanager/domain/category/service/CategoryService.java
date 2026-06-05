package com.storemanager.domain.category.service;

import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.repository.CategoryRepository;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.audit.service.AuditService;

import java.util.List;

public class CategoryService {

    private final CategoryRepository categoryRepository =
            new CategoryRepository();

    private final AuditService auditService =
            new AuditService();

    public List<Category> findAll() {

        validateCategoryViewAccess();

        return categoryRepository.findAll();
    }

    public boolean create(
            Category category
    ) {

        validateCategoryWriteAccess("CATEGORY_CREATE");
        validate(category);

        return categoryRepository.save(category);
    }

    public boolean update(
            Category category
    ) {

        validateCategoryWriteAccess("CATEGORY_UPDATE");

        if (category.getId() == null) {
            throw new RuntimeException(
                    "Category is required"
            );
        }

        validate(category);

        return categoryRepository.update(category);
    }

    public boolean delete(
            Category category
    ) {

        validateCategoryWriteAccess("CATEGORY_DELETE");

        if (category == null || category.getId() == null) {
            throw new RuntimeException(
                    "Category is required"
            );
        }

        return categoryRepository.delete(category);
    }

    public boolean deactivate(
            Category category
    ) {

        validateCategoryWriteAccess("CATEGORY_ARCHIVE");

        if (category == null || category.getId() == null) {
            throw new RuntimeException(
                    "Category is required"
            );
        }

        category.setActive(false);

        return update(category);
    }

    private void validate(
            Category category
    ) {

        if (category == null) {
            throw new RuntimeException(
                    "Category is required"
            );
        }

        if (category.getName() == null
                || category.getName().trim().isEmpty()) {
            throw new RuntimeException(
                    "Category name is required"
            );
        }

        category.setName(
                category.getName().trim()
        );

        category.setImagePath(
                cleanNullable(category.getImagePath())
        );
    }

    private void validateCategoryViewAccess() {

        if (!PermissionGuard.canViewProduct()) {
            auditService.recordPermissionDenied(
                    "CATEGORY_VIEW",
                    "CATEGORY",
                    null,
                    "Category access denied",
                    null,
                    "OWNER/MANAGER/STAFF/VIEWER"
            );
            throw new RuntimeException("Category access denied");
        }
    }

    private void validateCategoryWriteAccess(
            String action
    ) {

        if (!PermissionGuard.canModifyProduct()) {
            auditService.recordPermissionDenied(
                    action,
                    "CATEGORY",
                    null,
                    "Category modification denied",
                    null,
                    "OWNER/MANAGER"
            );
            throw new RuntimeException("Current user cannot modify categories");
        }
    }

    private String cleanNullable(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}

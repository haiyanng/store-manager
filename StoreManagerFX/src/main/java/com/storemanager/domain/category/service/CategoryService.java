package com.storemanager.domain.category.service;

import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.repository.CategoryRepository;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.audit.service.AuditSnapshots;

import java.util.List;
import java.util.Map;

public class CategoryService {

    private final CategoryRepository categoryRepository =
            new CategoryRepository();

    private final AuditService auditService =
            new AuditService();

    public List<Category> findAll() {

        validateCategoryViewAccess();

        return categoryRepository.findAll();
    }

    public boolean create(Category category) {
        return change(category, "CATEGORY_CREATE");
    }

    public boolean update(Category category) {
        return change(category, "CATEGORY_UPDATE");
    }

    public boolean delete(Category category) {
        return change(category, "CATEGORY_DELETE");
    }

    public boolean deactivate(Category category) {
        return change(category, "CATEGORY_ARCHIVE");
    }

    private boolean change(Category category, String action) {
        validateCategoryWriteAccess(action);
        Map<String, Object> before = null;
        Map<String, Object> attempted = AuditSnapshots.category(category);
        Map<String, Object> after;
        boolean success;
        try {
            if (category == null) throw new IllegalArgumentException("Category is required");
            boolean creating = action.endsWith("_CREATE");
            boolean deleting = action.endsWith("_DELETE");
            Category stored = null;
            if (!creating) {
                if (category.getId() == null) throw new IllegalArgumentException("Select a category first");
                stored = categoryRepository.findById(category.getId());
                if (stored == null) throw new IllegalArgumentException("Category no longer exists. Refresh the list and try again.");
                before = AuditSnapshots.category(stored);
            }
            Category candidate = category;
            if (action.endsWith("_ARCHIVE") || action.endsWith("_RESTORE")) {
                candidate = stored;
                candidate.setActive(action.endsWith("_RESTORE"));
            }
            if (!deleting) validate(candidate);
            attempted = deleting ? null : AuditSnapshots.category(candidate);
            success = creating ? categoryRepository.save(candidate)
                    : deleting ? categoryRepository.delete(candidate) : categoryRepository.update(candidate);
            after = success ? (deleting ? null : AuditSnapshots.category(candidate)) : before;
            if (success && !deleting) category.setActive(candidate.isActive());
        } catch (RuntimeException e) {
            auditService.recordChange("CATEGORY", action, "CATEGORY", category == null ? null : category.getId(),
                    false, e.getMessage(), before, before, attempted);
            throw e;
        }
        auditService.recordChange("CATEGORY", action, "CATEGORY", category.getId(), success,
                success ? null : "Unable to save category changes. Check the data and database connection.",
                before, after, success ? null : attempted);
        return success;
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

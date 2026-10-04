package com.storemanager.domain.product.service;

import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.service.CategoryService;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.audit.service.AuditSnapshots;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductService {

    private final ProductRepository productRepository =
            new ProductRepository();

    private final CategoryService categoryService =
            new CategoryService();

    private final AuditService auditService =
            new AuditService();

    public List<Product> findAll() {

        validateProductViewAccess();

        return productRepository.findAll();
    }

    public List<Product> findAllIncludingInactive() {

        validateProductViewAccess();

        return productRepository.findAllIncludingInactive();
    }

    public List<Category> findCategories() {

        validateProductViewAccess();

        return categoryService.findAll();
    }

    public Map<Long, Category> findCategoriesById() {

        return categoryService
                .findAll()
                .stream()
                .collect(
                        Collectors.toMap(
                                Category::getId,
                                category -> category
                        )
                );
    }

    public boolean create(Product product) {
        return change(product, "PRODUCT_CREATE");
    }

    public boolean update(Product product) {
        return change(product, "PRODUCT_UPDATE");
    }

    public boolean delete(Product product) {
        return change(product, "PRODUCT_ARCHIVE");
    }

    public boolean restore(Product product) {
        return change(product, "PRODUCT_RESTORE");
    }

    private boolean change(Product product, String action) {
        validateProductWriteAccess(action);
        Map<String, Object> before = null;
        Map<String, Object> attempted = AuditSnapshots.product(product);
        Map<String, Object> after;
        boolean success;
        try {
            if (product == null) throw new IllegalArgumentException("Product is required");
            boolean creating = action.endsWith("_CREATE");
            boolean deleting = action.endsWith("_DELETE");
            Product stored = null;
            if (!creating) {
                if (product.getId() == null) throw new IllegalArgumentException("Select a product first");
                stored = productRepository.findById(product.getId());
                if (stored == null) throw new IllegalArgumentException("Product no longer exists. Refresh the list and try again.");
                before = AuditSnapshots.product(stored);
            }
            Product candidate = product;
            if (action.endsWith("_ARCHIVE") || action.endsWith("_RESTORE")) {
                candidate = stored;
                candidate.setActive(action.endsWith("_RESTORE"));
            }
            if (!deleting) validate(candidate);
            attempted = deleting ? null : AuditSnapshots.product(candidate);
            success = creating ? productRepository.save(candidate)
                    : deleting ? productRepository.delete(candidate) : productRepository.update(candidate);
            after = success ? (deleting ? null : AuditSnapshots.product(candidate)) : before;
            if (success && !deleting) product.setActive(candidate.isActive());
        } catch (RuntimeException e) {
            auditService.recordChange("PRODUCT", action, "PRODUCT", product == null ? null : product.getId(),
                    false, e.getMessage(), before, before, attempted);
            throw e;
        }
        auditService.recordChange("PRODUCT", action, "PRODUCT", product.getId(), success,
                success ? null : "Unable to save product changes. Check the data and database connection.",
                before, after, success ? null : attempted);
        return success;
    }

    private void validate(
            Product product
    ) {

        if (product == null) {
            throw new RuntimeException(
                    "Product is required"
            );
        }

        if (product.getName() == null
                || product.getName().trim().isEmpty()) {
            throw new RuntimeException(
                    "Product name is required"
            );
        }

        if (product.getSku() == null
                || product.getSku().trim().isEmpty()) {
            throw new RuntimeException(
                    "SKU is required"
            );
        }

        if (product.getUnit() == null
                || product.getUnit().trim().isEmpty()) {
            throw new RuntimeException(
                    "Unit is required"
            );
        }

        if (product.getBasePrice() == null) {
            product.setBasePrice(
                    BigDecimal.ZERO
            );
        }

        if (product.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(
                    "Base price cannot be negative"
            );
        }

        if (product.getBasePrice().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Base price must have at most 2 decimal places");
        }

        product.setName(product.getName().trim());
        product.setSku(product.getSku().trim());
        if (product.getSku().length() > 80) {
            throw new IllegalArgumentException("SKU must contain at most 80 characters");
        }
        if (productRepository.existsSku(product.getSku(), product.getId())) {
            throw new IllegalArgumentException("SKU '" + product.getSku()
                    + "' is already assigned to another product. Use a different SKU.");
        }
        product.setUnit(product.getUnit().trim());
        product.setImagePath(cleanNullable(product.getImagePath()));
    }

    private void validateProductViewAccess() {

        if (!PermissionGuard.canViewProduct()) {
            auditService.recordPermissionDenied("PRODUCT_VIEW", "PRODUCT", null, "Product access denied", "OWNER/MANAGER/STAFF/VIEWER");
            throw new RuntimeException("Product access denied");
        }
    }

    private void validateProductWriteAccess(
            String action
    ) {

        if (!PermissionGuard.canModifyProduct()) {
            auditService.recordPermissionDenied(action, "PRODUCT", null, "Product modification denied", "OWNER/MANAGER");
            throw new RuntimeException("Current user cannot modify products");
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

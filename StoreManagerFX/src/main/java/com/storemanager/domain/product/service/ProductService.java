package com.storemanager.domain.product.service;

import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.service.CategoryService;
import com.storemanager.domain.audit.service.AuditService;
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

    public boolean create(
            Product product
    ) {

        validateProductWriteAccess("PRODUCT_CREATE");
        validate(product);

        boolean created =
                productRepository.save(product);

        auditService.recordEvent(
                "PRODUCT",
                "PRODUCT_CREATE",
                "PRODUCT",
                product.getId(),
                created,
                created ? null : "Product create failed",
                "{\"sku\":\"" + escape(product.getSku()) + "\"}"
        );

        return created;
    }

    public boolean update(
            Product product
    ) {

        validateProductWriteAccess("PRODUCT_UPDATE");

        if (product.getId() == null) {
            throw new RuntimeException(
                    "Product is required"
            );
        }

        validate(product);

        boolean updated =
                productRepository.update(product);

        auditService.recordEvent(
                "PRODUCT",
                "PRODUCT_UPDATE",
                "PRODUCT",
                product.getId(),
                updated,
                updated ? null : "Product update failed",
                "{\"sku\":\"" + escape(product.getSku()) + "\"}"
        );

        return updated;
    }

    public boolean delete(
            Product product
    ) {

        validateProductWriteAccess("PRODUCT_ARCHIVE");

        if (product == null || product.getId() == null) {
            throw new RuntimeException(
                    "Product is required"
            );
        }

        boolean archived =
                productRepository.delete(product);

        auditService.recordEvent(
                "PRODUCT",
                "PRODUCT_ARCHIVE",
                "PRODUCT",
                product.getId(),
                archived,
                archived ? null : "Product archive failed",
                "{\"sku\":\"" + escape(product.getSku()) + "\"}"
        );

        return archived;
    }

    public boolean restore(
            Product product
    ) {

        validateProductWriteAccess("PRODUCT_RESTORE");

        if (product == null || product.getId() == null) {
            throw new RuntimeException(
                    "Product is required"
            );
        }

        product.setActive(true);

        boolean restored =
                productRepository.update(product);

        auditService.recordEvent(
                "PRODUCT",
                "PRODUCT_RESTORE",
                "PRODUCT",
                product.getId(),
                restored,
                restored ? null : "Product restore failed",
                "{\"sku\":\"" + escape(product.getSku()) + "\"}"
        );

        return restored;
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

        product.setName(product.getName().trim());
        product.setSku(product.getSku().trim());
        product.setBarcode(cleanNullable(product.getBarcode()));
        product.setUnit(product.getUnit().trim());
        product.setImagePath(cleanNullable(product.getImagePath()));
    }

    private void validateProductViewAccess() {

        if (!PermissionGuard.canViewProduct()) {
            auditService.recordPermissionDenied(
                    "PRODUCT_VIEW",
                    "PRODUCT",
                    null,
                    "Product access denied",
                    null,
                    "OWNER/MANAGER/STAFF/VIEWER"
            );
            throw new RuntimeException("Product access denied");
        }
    }

    private void validateProductWriteAccess(
            String action
    ) {

        if (!PermissionGuard.canModifyProduct()) {
            auditService.recordPermissionDenied(
                    action,
                    "PRODUCT",
                    null,
                    "Product modification denied",
                    null,
                    "OWNER/MANAGER"
            );
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

    private String escape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}

package com.storemanager.domain.product.service;

import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.service.CategoryService;
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

    public List<Product> findAll() {

        return productRepository.findAll();
    }

    public List<Category> findCategories() {

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

        validate(product);

        return productRepository.save(product);
    }

    public boolean update(
            Product product
    ) {

        if (product.getId() == null) {
            throw new RuntimeException(
                    "Product is required"
            );
        }

        validate(product);

        return productRepository.update(product);
    }

    public boolean delete(
            Product product
    ) {

        if (product == null || product.getId() == null) {
            throw new RuntimeException(
                    "Product is required"
            );
        }

        return productRepository.delete(product);
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

    private String cleanNullable(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}

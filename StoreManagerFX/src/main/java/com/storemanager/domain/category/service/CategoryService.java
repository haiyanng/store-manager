package com.storemanager.domain.category.service;

import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.repository.CategoryRepository;

import java.util.List;

public class CategoryService {

    private final CategoryRepository categoryRepository =
            new CategoryRepository();

    public List<Category> findAll() {

        return categoryRepository.findAll();
    }

    public boolean create(
            Category category
    ) {

        validate(category);

        return categoryRepository.save(category);
    }

    public boolean update(
            Category category
    ) {

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

    private String cleanNullable(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}

package com.storeapi.catalog.service;

import com.storeapi.catalog.dto.CategoryDto;
import com.storeapi.catalog.dto.ProductDto;
import com.storeapi.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {
    private final CatalogRepository catalog;

    public CatalogService(CatalogRepository catalog) {
        this.catalog = catalog;
    }

    public List<ProductDto> products(String search, Long categoryId, String sort) {
        return catalog.products(search, categoryId, sort);
    }

    public ProductDto product(Long id) {
        return catalog.product(id).orElseThrow();
    }

    public List<CategoryDto> categories() {
        return catalog.categories();
    }
}

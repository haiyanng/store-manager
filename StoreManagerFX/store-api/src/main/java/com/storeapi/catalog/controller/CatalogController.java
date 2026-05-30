package com.storeapi.catalog.controller;

import com.storeapi.catalog.dto.CategoryDto;
import com.storeapi.catalog.dto.ProductDto;
import com.storeapi.catalog.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    List<ProductDto> products(@RequestParam(required = false) String search,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(required = false) String sort) {
        return catalog.products(search, categoryId, sort);
    }

    @GetMapping("/products/{id}")
    ProductDto product(@PathVariable Long id) {
        return catalog.product(id);
    }

    @GetMapping("/categories")
    List<CategoryDto> categories() {
        return catalog.categories();
    }
}

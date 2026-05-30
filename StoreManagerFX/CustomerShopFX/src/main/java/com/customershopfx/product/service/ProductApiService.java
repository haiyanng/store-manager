package com.customershopfx.product.service;

import com.customershopfx.app.AppContext;
import com.customershopfx.common.api.ApiClient;
import com.customershopfx.product.model.Category;
import com.customershopfx.product.model.Product;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductApiService {
    private final ApiClient api = AppContext.apiClient();

    public List<Product> products(String search, Long categoryId, String sort) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("search", search);
        params.put("categoryId", categoryId == null ? null : categoryId.toString());
        params.put("sort", sort);
        return api.get("/api/products" + api.query(params), new TypeReference<>() {});
    }

    public Product product(Long id) {
        return api.get("/api/products/" + id, new TypeReference<>() {});
    }

    public List<Category> categories() {
        return api.get("/api/categories", new TypeReference<>() {});
    }
}

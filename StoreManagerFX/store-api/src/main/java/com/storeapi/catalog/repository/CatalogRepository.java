package com.storeapi.catalog.repository;

import com.storeapi.catalog.dto.CategoryDto;
import com.storeapi.catalog.dto.ProductDto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CatalogRepository {
    private final JdbcClient jdbc;

    public CatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProductDto> products(String search, Long categoryId, String sort) {
        String orderBy = switch (sort == null ? "" : sort) {
            case "price_desc" -> "p.base_price DESC, p.name";
            case "price_asc" -> "p.base_price ASC, p.name";
            case "name_desc" -> "p.name DESC";
            default -> "p.name ASC";
        };
        return jdbc.sql("""
                SELECT p.*, c.name AS category_name
                FROM products p
                LEFT JOIN categories c ON c.id = p.category_id
                WHERE p.active = TRUE
                  AND (:search IS NULL OR LOWER(p.name) LIKE CONCAT('%%', LOWER(:search), '%%')
                       OR LOWER(p.sku) LIKE CONCAT('%%', LOWER(:search), '%%'))
                  AND (:categoryId IS NULL OR p.category_id = :categoryId)
                ORDER BY %s
                """.formatted(orderBy))
                .param("search", blankToNull(search))
                .param("categoryId", categoryId)
                .query((rs, rowNum) -> new ProductDto(rs.getLong("id"), rs.getString("name"), rs.getString("sku"),
                        rs.getString("barcode"), readLong(rs, "category_id"), rs.getString("category_name"),
                        rs.getBigDecimal("base_price"), rs.getString("unit"), rs.getString("image_path")))
                .list();
    }

    public Optional<ProductDto> product(Long id) {
        return jdbc.sql("""
                SELECT p.*, c.name AS category_name
                FROM products p
                LEFT JOIN categories c ON c.id = p.category_id
                WHERE p.id = :id AND p.active = TRUE
                """)
                .param("id", id)
                .query((rs, rowNum) -> new ProductDto(rs.getLong("id"), rs.getString("name"), rs.getString("sku"),
                        rs.getString("barcode"), readLong(rs, "category_id"), rs.getString("category_name"),
                        rs.getBigDecimal("base_price"), rs.getString("unit"), rs.getString("image_path")))
                .optional();
    }

    public List<CategoryDto> categories() {
        return jdbc.sql("SELECT id, name, image_path FROM categories WHERE active = TRUE ORDER BY name")
                .query((rs, rowNum) -> new CategoryDto(rs.getLong("id"), rs.getString("name"), rs.getString("image_path")))
                .list();
    }

    private Long readLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

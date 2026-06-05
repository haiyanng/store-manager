package com.storeapi.catalog.dto;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, String sku, String barcode, Long categoryId,
                         String categoryName, BigDecimal basePrice, String unit, String imagePath,
                         String shortDescription, String fullDescription) {
}

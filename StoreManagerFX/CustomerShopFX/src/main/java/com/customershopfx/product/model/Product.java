package com.customershopfx.product.model;

import java.math.BigDecimal;

public record Product(Long id, String name, String sku, String barcode, Long categoryId, String categoryName,
                      BigDecimal basePrice, String unit, String imagePath, String shortDescription,
                      String fullDescription) {
}

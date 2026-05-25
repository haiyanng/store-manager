package com.storemanager.domain.sale.model;

import java.math.BigDecimal;

public record SelectedProductPreviewDto(
        Long productId,
        String name,
        String sku,
        String barcode,
        String imagePath,
        BigDecimal unitPrice,
        Integer stockQuantity
) {
}

package com.customershopfx.product.model;

public record Category(Long id, String name, String imagePath) {
    @Override
    public String toString() {
        return name;
    }
}

package com.customershopfx.cart.model;

import java.math.BigDecimal;
import java.util.List;

public record Cart(Long id, List<CartItem> items, BigDecimal total) {
}

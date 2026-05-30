package com.storeapi.cart.repository;

import com.storeapi.cart.dto.CartDto;
import com.storeapi.cart.dto.CartItemDto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class CartRepository {
    private final JdbcClient jdbc;

    public CartRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Long cartId(Long customerId) {
        jdbc.sql("INSERT IGNORE INTO carts (customer_id) VALUES (:customerId)")
                .param("customerId", customerId)
                .update();
        return jdbc.sql("SELECT id FROM carts WHERE customer_id = :customerId")
                .param("customerId", customerId)
                .query(Long.class)
                .single();
    }

    public CartDto cart(Long customerId) {
        Long cartId = cartId(customerId);
        List<CartItemDto> items = jdbc.sql("""
                SELECT ci.id, p.id AS product_id, p.name, p.base_price, ci.quantity,
                       p.image_path, (p.base_price * ci.quantity) AS subtotal
                FROM cart_items ci
                JOIN products p ON p.id = ci.product_id
                WHERE ci.cart_id = :cartId
                ORDER BY ci.id
                """)
                .param("cartId", cartId)
                .query((rs, rowNum) -> new CartItemDto(rs.getLong("id"), rs.getLong("product_id"),
                        rs.getString("name"), rs.getBigDecimal("base_price"), rs.getInt("quantity"),
                        rs.getBigDecimal("subtotal"), rs.getString("image_path")))
                .list();
        BigDecimal total = items.stream().map(CartItemDto::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDto(cartId, items, total);
    }

    public void addItem(Long customerId, Long productId, int quantity) {
        Long cartId = cartId(customerId);
        jdbc.sql("""
                INSERT INTO cart_items (cart_id, product_id, quantity)
                SELECT :cartId, p.id, :quantity
                FROM products p
                WHERE p.id = :productId AND p.active = TRUE
                ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)
                """)
                .param("cartId", cartId)
                .param("productId", productId)
                .param("quantity", quantity)
                .update();
    }

    public void updateItem(Long customerId, Long itemId, int quantity) {
        jdbc.sql("""
                UPDATE cart_items ci
                JOIN carts c ON c.id = ci.cart_id
                SET ci.quantity = :quantity
                WHERE ci.id = :itemId AND c.customer_id = :customerId
                """)
                .param("quantity", quantity)
                .param("itemId", itemId)
                .param("customerId", customerId)
                .update();
    }

    public void removeItem(Long customerId, Long itemId) {
        jdbc.sql("""
                DELETE ci FROM cart_items ci
                JOIN carts c ON c.id = ci.cart_id
                WHERE ci.id = :itemId AND c.customer_id = :customerId
                """)
                .param("itemId", itemId)
                .param("customerId", customerId)
                .update();
    }

    public void clear(Long customerId) {
        jdbc.sql("DELETE ci FROM cart_items ci JOIN carts c ON c.id = ci.cart_id WHERE c.customer_id = :customerId")
                .param("customerId", customerId)
                .update();
    }
}

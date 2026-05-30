package com.storeapi.order.repository;

import com.storeapi.order.dto.CheckoutRequest;
import com.storeapi.order.dto.OrderDto;
import com.storeapi.order.dto.OrderItemDto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {
    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Long create(Long customerId, CheckoutRequest request, java.math.BigDecimal total) {
        jdbc.sql("""
                INSERT INTO orders (customer_id, status, total_amount, recipient_name, phone, shipping_address, payment_method)
                VALUES (:customerId, 'PENDING', :total, :recipientName, :phone, :shippingAddress, :paymentMethod)
                """)
                .param("customerId", customerId)
                .param("total", total)
                .param("recipientName", request.recipientName())
                .param("phone", request.phone())
                .param("shippingAddress", request.shippingAddress())
                .param("paymentMethod", request.paymentMethod())
                .update();
        return jdbc.sql("SELECT LAST_INSERT_ID()").query(Long.class).single();
    }

    public void addItem(Long orderId, Long productId, String productName, int quantity,
                        java.math.BigDecimal unitPrice, java.math.BigDecimal subtotal) {
        jdbc.sql("""
                INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, subtotal)
                VALUES (:orderId, :productId, :productName, :quantity, :unitPrice, :subtotal)
                """)
                .param("orderId", orderId)
                .param("productId", productId)
                .param("productName", productName)
                .param("quantity", quantity)
                .param("unitPrice", unitPrice)
                .param("subtotal", subtotal)
                .update();
    }

    public List<OrderDto> myOrders(Long customerId) {
        return jdbc.sql("""
                SELECT * FROM orders
                WHERE customer_id = :customerId
                ORDER BY created_at DESC, id DESC
                """)
                .param("customerId", customerId)
                .query((rs, rowNum) -> new OrderDto(rs.getLong("id"), rs.getString("status"),
                        rs.getBigDecimal("total_amount"), rs.getString("recipient_name"), rs.getString("phone"),
                        rs.getString("shipping_address"), rs.getString("payment_method"),
                        toLocal(rs.getTimestamp("created_at")), List.of()))
                .list();
    }

    public Optional<OrderDto> find(Long customerId, Long orderId) {
        return jdbc.sql("SELECT * FROM orders WHERE id = :id AND customer_id = :customerId")
                .param("id", orderId)
                .param("customerId", customerId)
                .query((rs, rowNum) -> new OrderDto(rs.getLong("id"), rs.getString("status"),
                        rs.getBigDecimal("total_amount"), rs.getString("recipient_name"), rs.getString("phone"),
                        rs.getString("shipping_address"), rs.getString("payment_method"),
                        toLocal(rs.getTimestamp("created_at")), items(orderId)))
                .optional();
    }

    public boolean cancel(Long customerId, Long orderId) {
        return jdbc.sql("""
                UPDATE orders
                SET status = 'CANCELLED'
                WHERE id = :id AND customer_id = :customerId AND status = 'PENDING'
                """)
                .param("id", orderId)
                .param("customerId", customerId)
                .update() > 0;
    }

    private List<OrderItemDto> items(Long orderId) {
        return jdbc.sql("SELECT * FROM order_items WHERE order_id = :orderId ORDER BY id")
                .param("orderId", orderId)
                .query((rs, rowNum) -> new OrderItemDto(rs.getLong("id"), rs.getLong("product_id"),
                        rs.getString("product_name"), rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
                        rs.getBigDecimal("subtotal")))
                .list();
    }

    private java.time.LocalDateTime toLocal(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}

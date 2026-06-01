package com.storemanager.domain.onlineorder.dao;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.util.TimeFormatUtil;
import com.storemanager.domain.onlineorder.model.OnlineOrder;
import com.storemanager.domain.onlineorder.model.OnlineOrderSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OnlineOrderDAO {

    public List<OnlineOrderSummary> findAll() {
        List<OnlineOrderSummary> orders = new ArrayList<>();

        String sql = """
                SELECT
                    o.id,
                    c.full_name AS customer_full_name,
                    c.email AS customer_email,
                    o.status,
                    o.total_amount,
                    o.created_at
                FROM orders o
                INNER JOIN customers c ON c.id = o.customer_id
                ORDER BY o.created_at DESC, o.id DESC
                """;

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                orders.add(mapSummary(resultSet));
            }
            return orders;
        } catch (Exception e) {
            throw new OnlineOrderDataAccessException("Cannot load online orders", e);
        }
    }

    public Optional<OnlineOrder> findById(Long orderId) {
        if (orderId == null) {
            return Optional.empty();
        }

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement orderStatement = connection.prepareStatement("""
                        SELECT
                            o.id,
                            o.customer_id,
                            o.status,
                            o.total_amount,
                            o.created_at,
                            o.recipient_name,
                            o.phone,
                            o.shipping_address,
                            o.payment_method
                        FROM orders o
                        WHERE o.id = ?
                        """)
        ) {
            orderStatement.setLong(1, orderId);

            try (ResultSet orderResultSet = orderStatement.executeQuery()) {
                if (!orderResultSet.next()) {
                    return Optional.empty();
                }

                Long customerId = readNullableLong(orderResultSet, "customer_id");
                CustomerRow customer = findCustomer(connection, customerId);
                if (customer == null) {
                    throw new OnlineOrderDataAccessException(
                            "Customer record missing for online order " + orderId,
                            null
                    );
                }

                return Optional.of(mapOrder(orderResultSet, customer));
            }
        } catch (Exception e) {
            if (e instanceof OnlineOrderDataAccessException) {
                throw (OnlineOrderDataAccessException) e;
            }
            throw new OnlineOrderDataAccessException("Cannot load online order " + orderId, e);
        }
    }

    public boolean updateStatus(Long orderId, String currentStatus, String nextStatus) {
        if (orderId == null || currentStatus == null || currentStatus.isBlank() || nextStatus == null || nextStatus.isBlank()) {
            return false;
        }

        String sql = """
                UPDATE orders
                SET status = ?
                WHERE id = ?
                  AND status = ?
                """;

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, nextStatus);
            statement.setLong(2, orderId);
            statement.setString(3, currentStatus);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            throw new OnlineOrderDataAccessException("Cannot update online order status", e);
        }
    }

    private OnlineOrderSummary mapSummary(ResultSet resultSet) throws Exception {
        OnlineOrderSummary summary = new OnlineOrderSummary();
        summary.setId(resultSet.getLong("id"));
        summary.setCustomerFullName(resultSet.getString("customer_full_name"));
        summary.setCustomerEmail(resultSet.getString("customer_email"));
        summary.setStatus(resultSet.getString("status"));
        summary.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        summary.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        return summary;
    }

    private OnlineOrder mapOrder(ResultSet resultSet, CustomerRow customer) throws Exception {
        OnlineOrder order = new OnlineOrder();
        order.setId(resultSet.getLong("id"));
        order.setCustomerId(resultSet.getLong("customer_id"));
        order.setCustomerFullName(customer.fullName());
        order.setCustomerEmail(customer.email());
        order.setStatus(resultSet.getString("status"));
        order.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        order.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        order.setRecipientName(resultSet.getString("recipient_name"));
        order.setPhone(resultSet.getString("phone"));
        order.setShippingAddress(resultSet.getString("shipping_address"));
        order.setPaymentMethod(resultSet.getString("payment_method"));
        return order;
    }

    private CustomerRow findCustomer(Connection connection, Long customerId) throws Exception {
        if (customerId == null) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT full_name, email
                FROM customers
                WHERE id = ?
                """)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new CustomerRow(
                        resultSet.getString("full_name"),
                        resultSet.getString("email")
                );
            }
        }
    }

    private Long readNullableLong(ResultSet resultSet, String column) throws Exception {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private java.time.LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : TimeFormatUtil.truncateToSeconds(timestamp.toLocalDateTime());
    }

    private record CustomerRow(String fullName, String email) {
    }
}

package com.storemanager.domain.onlineorder.dao;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.onlineorder.model.OnlineOrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OnlineOrderItemDAO {

    public List<OnlineOrderItem> findItemsByOrderId(Long orderId) {
        List<OnlineOrderItem> items = new ArrayList<>();

        if (orderId == null) {
            return items;
        }

        String sql = """
                SELECT
                    id,
                    order_id,
                    product_id,
                    product_name,
                    quantity,
                    unit_price,
                    subtotal
                FROM order_items
                WHERE order_id = ?
                ORDER BY id
                """;

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, orderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapItem(resultSet));
                }
            }
            return items;
        } catch (Exception e) {
            throw new OnlineOrderDataAccessException("Cannot load online order items", e);
        }
    }

    private OnlineOrderItem mapItem(ResultSet resultSet) throws Exception {
        OnlineOrderItem item = new OnlineOrderItem();
        item.setId(resultSet.getLong("id"));
        item.setOrderId(resultSet.getLong("order_id"));
        item.setProductId(resultSet.getLong("product_id"));
        item.setProductName(resultSet.getString("product_name"));
        item.setQuantity(resultSet.getInt("quantity"));
        item.setUnitPrice(resultSet.getBigDecimal("unit_price"));
        item.setSubtotal(resultSet.getBigDecimal("subtotal"));
        return item;
    }
}

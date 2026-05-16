package com.storemanager.domain.sale.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.sale.model.SaleOrder;
import com.storemanager.domain.sale.model.SaleOrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SaleRepository {

    public SaleRepository() {

        initializeTables();
    }

    public Long saveOrder(
            SaleOrder order,
            List<SaleOrderItem> items
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection()
        ) {

            connection.setAutoCommit(false);

            try {

                Long orderId =
                        insertOrder(
                                connection,
                                order
                        );

                for (SaleOrderItem item : items) {
                    item.setOrderId(orderId);
                    insertOrderItem(
                            connection,
                            item
                    );
                }

                connection.commit();

                return orderId;

            } catch (Exception e) {

                connection.rollback();
                throw e;
            }

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    public List<SaleOrder> findRecentOrders() {

        List<SaleOrder> orders =
                new ArrayList<>();

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement();

                ResultSet resultSet =
                        statement.executeQuery(
                                """
                                SELECT *
                                FROM sale_orders
                                ORDER BY created_at DESC, id DESC
                                LIMIT 50
                                """
                        )
        ) {

            while (resultSet.next()) {
                orders.add(
                        mapOrder(resultSet)
                );
            }

            return orders;

        } catch (Exception e) {

            e.printStackTrace();

            return orders;
        }
    }

    private void initializeTables() {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS sale_orders (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        created_by_user_id BIGINT,
                        total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS sale_order_items (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        order_id BIGINT NOT NULL,
                        product_id BIGINT NOT NULL,
                        quantity INT NOT NULL,
                        unit_price DECIMAL(18, 2) NOT NULL,
                        subtotal DECIMAL(18, 2) NOT NULL
                    )
                    """
            );

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Sale table initialization failed",
                    e
            );
        }
    }

    private Long insertOrder(
            Connection connection,
            SaleOrder order
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO sale_orders (
                                    created_by_user_id,
                                    total_amount
                                )
                                VALUES (?, ?)
                                """,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            if (order.getCreatedByUserId() == null) {
                statement.setObject(
                        1,
                        null
                );
            } else {
                statement.setLong(
                        1,
                        order.getCreatedByUserId()
                );
            }

            statement.setBigDecimal(
                    2,
                    order.getTotalAmount()
            );

            statement.executeUpdate();

            ResultSet keys =
                    statement.getGeneratedKeys();

            if (!keys.next()) {
                throw new RuntimeException(
                        "Cannot create sale order"
                );
            }

            return keys.getLong(1);
        }
    }

    private void insertOrderItem(
            Connection connection,
            SaleOrderItem item
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO sale_order_items (
                                    order_id,
                                    product_id,
                                    quantity,
                                    unit_price,
                                    subtotal
                                )
                                VALUES (?, ?, ?, ?, ?)
                                """
                        )
        ) {

            statement.setLong(1, item.getOrderId());
            statement.setLong(2, item.getProductId());
            statement.setInt(3, item.getQuantity());
            statement.setBigDecimal(4, item.getUnitPrice());
            statement.setBigDecimal(5, item.getSubtotal());
            statement.executeUpdate();
        }
    }

    private SaleOrder mapOrder(
            ResultSet resultSet
    ) throws Exception {

        SaleOrder order =
                new SaleOrder();

        order.setId(resultSet.getLong("id"));

        long createdByUserId =
                resultSet.getLong("created_by_user_id");

        if (resultSet.wasNull()) {
            order.setCreatedByUserId(null);
        } else {
            order.setCreatedByUserId(createdByUserId);
        }

        order.setTotalAmount(
                resultSet.getBigDecimal("total_amount")
        );

        Timestamp createdAt =
                resultSet.getTimestamp("created_at");

        if (createdAt != null) {
            order.setCreatedAt(
                    createdAt.toLocalDateTime()
            );
        }

        return order;
    }
}

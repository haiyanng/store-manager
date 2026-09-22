package com.storemanager.domain.dashboard.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.util.TimeFormatUtil;
import com.storemanager.domain.dashboard.model.DashboardOnlineOrderRow;
import com.storemanager.domain.dashboard.model.DashboardOnlineOrderSnapshot;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DashboardOnlineOrderRepository {

    public DashboardOnlineOrderSnapshot loadSnapshot(
            int recentLimit
    ) {

        DashboardOnlineOrderSnapshot snapshot =
                new DashboardOnlineOrderSnapshot();

        String summarySql =
                """
                SELECT
                    COUNT(*) AS total_orders,
                    COALESCE(SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending_orders,
                    COALESCE(SUM(CASE WHEN status = 'CONFIRMED' THEN 1 ELSE 0 END), 0) AS confirmed_orders,
                    COALESCE(SUM(CASE WHEN status = 'DELIVERING' THEN 1 ELSE 0 END), 0) AS delivering_orders,
                    COALESCE(SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END), 0) AS delivered_orders,
                    COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_orders,
                    COALESCE(SUM(CASE WHEN status = 'DELIVERED' THEN total_amount ELSE 0 END), 0) AS total_revenue,
                    COALESCE(SUM(CASE WHEN status = 'DELIVERED' AND DATE(created_at) = CURRENT_DATE THEN total_amount ELSE 0 END), 0) AS revenue_today,
                    COALESCE(SUM(CASE WHEN DATE(created_at) = CURRENT_DATE THEN 1 ELSE 0 END), 0) AS orders_today
                FROM orders
                """;

        String recentSql =
                """
                SELECT
                    o.id,
                    c.full_name AS customer_name,
                    o.status,
                    o.total_amount,
                    o.created_at
                FROM orders o
                INNER JOIN customers c ON c.id = o.customer_id
                ORDER BY o.created_at DESC, o.id DESC
                LIMIT ?
                """;

        try (
                Connection connection = ConnectionFactory.getConnection();
                PreparedStatement summaryStatement = connection.prepareStatement(summarySql);
                ResultSet summaryResult = summaryStatement.executeQuery()
        ) {
            if (summaryResult.next()) {
                snapshot.setTotalOrders(summaryResult.getLong("total_orders"));
                snapshot.setPendingOrders(summaryResult.getLong("pending_orders"));
                snapshot.setConfirmedOrders(summaryResult.getLong("confirmed_orders"));
                snapshot.setDeliveringOrders(summaryResult.getLong("delivering_orders"));
                snapshot.setDeliveredOrders(summaryResult.getLong("delivered_orders"));
                snapshot.setCancelledOrders(summaryResult.getLong("cancelled_orders"));
                snapshot.setTotalRevenue(summaryResult.getBigDecimal("total_revenue"));
                snapshot.setRevenueToday(summaryResult.getBigDecimal("revenue_today"));
                snapshot.setOrdersToday(summaryResult.getLong("orders_today"));
            }

            try (
                    PreparedStatement recentStatement = connection.prepareStatement(recentSql)
            ) {
                recentStatement.setInt(1, Math.max(recentLimit, 1));

                try (ResultSet recentResult = recentStatement.executeQuery()) {
                    snapshot.setRecentOrders(readRecentOrders(recentResult));
                }
            }

            return snapshot;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load online order dashboard", e);
        }
    }

    private List<DashboardOnlineOrderRow> readRecentOrders(
            ResultSet resultSet
    ) throws Exception {

        List<DashboardOnlineOrderRow> rows = new ArrayList<>();

        while (resultSet.next()) {
            DashboardOnlineOrderRow row = new DashboardOnlineOrderRow();
            row.setOrderId(resultSet.getLong("id"));
            row.setCustomerName(resultSet.getString("customer_name"));
            row.setStatus(resultSet.getString("status"));
            row.setTotalAmount(resultSet.getBigDecimal("total_amount"));
            row.setCreatedAt(
                    resultSet.getTimestamp("created_at") == null
                            ? null
                            : TimeFormatUtil.truncateToSeconds(
                                    resultSet.getTimestamp("created_at").toLocalDateTime()
                            )
            );
            rows.add(row);
        }

        return rows;
    }
}

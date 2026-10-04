package com.storemanager.refactor;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.database.DatabaseInitializer;
import com.storemanager.core.security.PasswordHasher;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.inventory.model.*;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.sale.model.*;
import com.storemanager.domain.sale.service.*;
import com.storemanager.domain.user.model.*;
import com.storemanager.domain.user.repository.UserRepository;
import com.storemanager.domain.user.service.PasswordChangeService;
import com.storemanager.domain.dashboard.service.DashboardAnalyticsService;
import org.junit.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.Assert.*;

public class SalesWorkflowTest {
    @BeforeClass public static void setup() {
        ConnectionFactory.setSettings(new DatabaseSettings() {
            public String buildDatabaseUrl() { return "jdbc:h2:mem:sales-workflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"; }
            public String getUsername() { return "sa"; }
            public String getPassword() { return ""; }
        });
        DatabaseInitializer.initialize();
    }
    @AfterClass public static void close() throws Exception {
        try (var c = ConnectionFactory.getConnection(); var s = c.createStatement()) { s.execute("SHUTDOWN"); }
        AppSession.clear(); ConnectionFactory.setSettings(null);
    }

    @Test public void importFiltersExcludeSalesAndIncludeBothDateBoundaries() {
        LocalDate date = LocalDate.of(2026, 10, 4);
        var first = row(1, InventoryTransactionType.IMPORT, date);
        var last = row(1, InventoryTransactionType.IMPORT, date.plusDays(1));
        var rows = List.of(first, last, row(1, InventoryTransactionType.SALE, date),
                row(2, InventoryTransactionType.IMPORT, date), row(1, InventoryTransactionType.IMPORT, date.minusDays(1)));
        assertEquals(List.of(first, last), InventoryService.filterImportHistory(rows, date, date.plusDays(1), 1L));
        assertEquals(4, InventoryService.filterImportHistory(rows, null, null, null).size());
        assertThrows(IllegalArgumentException.class, () -> InventoryService.filterImportHistory(rows, date.plusDays(1), date, null));
        InventoryItem item = new InventoryItem();
        item.setQuantity(9); assertTrue(InventoryService.isLowStock(item));
        item.setQuantity(10); assertFalse(InventoryService.isLowStock(item));
        item.setQuantity(0); assertTrue(InventoryService.isLowStock(item));
    }

    @Test public void stockMessageAndInvoiceContent() {
        var error = assertThrows(IllegalArgumentException.class, () -> SaleService.validateStock("Bánh mì", 9, 11));
        assertEquals("Insufficient stock for Bánh mì: available 9, requested 11, short by 2.", error.getMessage());
        SaleService.validateStock("Bánh mì", 9, 9);
        SaleOrder order = new SaleOrder(); order.setId(12L); order.setTotalAmount(BigDecimal.TEN);
        order.setAmountReceived(new BigDecimal("20")); order.setChangeAmount(BigDecimal.TEN);
        SaleOrderItemDetail detail = new SaleOrderItemDetail(); detail.setProductName("<script>alert(1)</script>");
        detail.setSku("A&B"); detail.setQuantity(1); detail.setUnitPrice(BigDecimal.TEN); detail.setSubtotal(BigDecimal.TEN);
        String html = InvoiceService.render(order, List.of(detail));
        assertTrue(html.contains("Invoice #12")); assertTrue(html.contains("Amount received"));
        assertTrue(html.contains("&lt;script&gt;")); assertFalse(html.contains("<script>"));
    }

    @Test public void staffCanChangeOwnPasswordWithoutUserManagementAccess() {
        User staff = new User(); staff.setUsername("password-test"); staff.setPassword(PasswordHasher.hash("old"));
        staff.setRole(RoleType.STAFF); staff.setActive(true);
        var repository = new UserRepository(); repository.save(staff); AppSession.setCurrentUser(staff);
        var service = new PasswordChangeService();
        assertThrows(IllegalArgumentException.class, () -> service.changePassword("wrong", "new", "new"));
        assertThrows(IllegalArgumentException.class, () -> service.changePassword("old", "new", "mismatch"));
        assertEquals(PasswordHasher.hash("old"), repository.findById(staff.getId()).orElseThrow().getPassword());
        service.changePassword("old", "new", "new");
        assertEquals(PasswordHasher.hash("new"), repository.findById(staff.getId()).orElseThrow().getPassword());
        assertEquals(RoleType.STAFF, repository.findById(staff.getId()).orElseThrow().getRole());
        assertFalse(repository.changePassword(staff.getId(), PasswordHasher.hash("old"), "wrong"));
        AppSession.clear();
        assertThrows(IllegalStateException.class, () -> service.changePassword("new", "next", "next"));
    }

    @Test public void dashboardShowsFourDailyMetricsAndIgnoresYesterday() throws Exception {
        AppSession.setCurrentUser(RoleAndNavigationTest.user(1, RoleType.OWNER));
        LocalDate date = LocalDate.now();
        try (var c = ConnectionFactory.getConnection(); var s = c.createStatement()) {
            s.execute("INSERT INTO sale_orders(total_amount,created_at) VALUES (25,'" + date + " 00:00:00'), (50,'" + date.minusDays(1) + " 23:59:59')");
        }
        var sales = new SaleService(); assertEquals(0, new BigDecimal("25").compareTo(sales.findRevenueToday()));
        assertEquals(1, sales.countOrdersToday());
        var metrics = new DashboardAnalyticsService().loadDashboardAnalytics().getMetrics();
        assertEquals(4, metrics.size());
        var saved = sales.findRecentOrders().stream().filter(o -> o.getTotalAmount().compareTo(new BigDecimal("25")) == 0)
                .findFirst().orElseThrow();
        var reference = new SaleOrder(); reference.setId(saved.getId()); reference.setTotalAmount(BigDecimal.ZERO);
        java.nio.file.Path destination = java.nio.file.Files.createTempFile("invoice-test", ".html");
        try {
            new InvoiceService().export(reference, destination);
            String html = java.nio.file.Files.readString(destination);
            assertTrue(html.contains("Date: " + com.storemanager.core.util.TimeFormatUtil.formatDateTime(saved.getCreatedAt())));
            assertTrue(html.contains("Total: " + com.storemanager.core.util.MoneyFormatUtil.format(saved.getTotalAmount())));
        } finally { java.nio.file.Files.deleteIfExists(destination); }
    }

    private static InventoryTransaction row(long id, InventoryTransactionType type, LocalDate date) {
        InventoryTransaction row = new InventoryTransaction(); row.setProductId(id); row.setType(type);
        row.setCreatedAt(date.atTime(23, 59)); return row;
    }
}

package com.storemanager.refactor;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.database.DatabaseInitializer;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.importing.model.ImportCartItem;
import com.storemanager.domain.importing.service.ImportService;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.sale.model.SaleCartItem;
import com.storemanager.domain.sale.repository.SaleRepository;
import com.storemanager.domain.sale.service.SaleService;
import com.storemanager.domain.user.model.RoleType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

public class PosAndExpiryTest {
    @BeforeClass
    public static void database() throws Exception {
        ConnectionFactory.setSettings(new DatabaseSettings() {
            @Override public String buildDatabaseUrl() {
                return "jdbc:h2:mem:pos-expiry;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
            }
            @Override public String getUsername() { return "sa"; }
            @Override public String getPassword() { return ""; }
        });
        // Start with populated legacy tables, without the new columns.
        sql("CREATE TABLE sale_orders (id BIGINT AUTO_INCREMENT PRIMARY KEY, created_by_user_id BIGINT, total_amount DECIMAL(18,2), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        sql("CREATE TABLE import_items (id BIGINT AUTO_INCREMENT PRIMARY KEY, import_receipt_id BIGINT, product_id BIGINT, quantity INT, unit_cost DECIMAL(18,2), subtotal DECIMAL(18,2))");
        sql("INSERT INTO sale_orders (id,total_amount) VALUES (9000,10)");
        sql("INSERT INTO import_items (import_receipt_id,product_id,quantity,unit_cost,subtotal) VALUES (9000,9000,1,10,10)");
        DatabaseInitializer.initialize();
        DatabaseInitializer.initialize(); // Migration must be repeatable.
    }

    @Before
    public void owner() { AppSession.setCurrentUser(RoleAndNavigationTest.user(1, RoleType.OWNER)); }

    @AfterClass
    public static void close() throws Exception {
        try { sql("SHUTDOWN"); }
        finally { AppSession.clear(); ConnectionFactory.setSettings(null); }
    }

    @Test
    public void migrationPreservesLegacyRows() throws Exception {
        var old = new SaleRepository().findRecentOrders().stream().filter(o -> o.getId() == 9000L).findFirst().orElseThrow();
        assertNull(old.getAmountReceived());
        assertNull(old.getChangeAmount());
        assertEquals(1, scalar("SELECT COUNT(*) FROM import_items WHERE product_id=9000 AND expiry_date IS NULL"));
    }

    @Test
    public void paymentValidationAndExpiryBoundaries() {
        BigDecimal total = new BigDecimal("1000.00");
        assertEquals(total, SaleService.parseAmountReceived("1,000.00"));
        SaleService.validatePayment(total, total);
        SaleService.validatePayment(total, new BigDecimal("1500"));
        for (String bad : List.of("", "abc", "-1", "1,00", "1.001", "1e3", "10000000000000000")) {
            assertThrows(IllegalArgumentException.class, () -> SaleService.parseAmountReceived(bad));
        }
        assertThrows(IllegalArgumentException.class, () -> SaleService.validatePayment(total, new BigDecimal("999.99")));
        LocalDate today = LocalDate.now();
        assertEquals("N/A", InventoryService.expiryStatus(null, today));
        assertEquals("EXPIRED", InventoryService.expiryStatus(today.minusDays(1), today));
        assertEquals("EXPIRING SOON", InventoryService.expiryStatus(today, today));
        assertEquals("EXPIRING SOON", InventoryService.expiryStatus(today.plusDays(7), today));
        assertEquals("NORMAL", InventoryService.expiryStatus(today.plusDays(8), today));
    }

    @Test
    public void staffPaymentPersistsAndFailedStockWriteRollsBack() throws Exception {
        Product product = product(8101);
        ImportCartItem imported = new ImportCartItem(product, 5, new BigDecimal("5.00"));
        imported.setExpiryDate(LocalDate.now().plusDays(3));
        new ImportService().finalizeImport("Test supplier", List.of(imported));
        AppSession.setCurrentUser(RoleAndNavigationTest.user(2, RoleType.STAFF));
        SaleService sales = new SaleService();
        assertThrows(RuntimeException.class, () -> sales.finalizeSale(List.of(), BigDecimal.TEN));
        assertThrows(RuntimeException.class, () -> sales.finalizeSale(List.of(new SaleCartItem(product, 1)), BigDecimal.ONE));
        int before = scalar("SELECT COUNT(*) FROM sale_orders");
        // Each row passes the initial stock check; the second write must fail and roll everything back.
        assertThrows(RuntimeException.class, () -> sales.finalizeSale(
                List.of(new SaleCartItem(product, 4), new SaleCartItem(product, 4)), new BigDecimal("100")));
        assertEquals(before, scalar("SELECT COUNT(*) FROM sale_orders"));
        assertEquals(5, scalar("SELECT quantity FROM inventory_items WHERE product_id=8101"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM inventory_transactions WHERE product_id=8101 AND type='SALE'"));
        var order = sales.finalizeSale(List.of(new SaleCartItem(product, 2)), new BigDecimal("30.00"));
        var saved = new SaleRepository().findRecentOrders().stream().filter(o -> o.getId().equals(order.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("20.00"), saved.getTotalAmount());
        assertEquals(new BigDecimal("30.00"), saved.getAmountReceived());
        assertEquals(new BigDecimal("10.00"), saved.getChangeAmount());
        assertEquals(Long.valueOf(2), saved.getCreatedByUserId());
        assertEquals(3, scalar("SELECT quantity FROM inventory_items WHERE product_id=8101"));
        var exact = sales.finalizeSale(List.of(new SaleCartItem(product, 1)), new BigDecimal("10.00"));
        assertEquals(0, exact.getChangeAmount().signum());
    }

    @Test
    public void importExpiryIsOptionalValidatedAndAggregatedFromHistory() throws Exception {
        Product product = product(8102);
        ImportService imports = new ImportService();
        ImportCartItem item = new ImportCartItem(product, 1, BigDecimal.ONE);
        item.setExpiryDate(LocalDate.now().minusDays(1));
        int before = scalar("SELECT COUNT(*) FROM import_receipts");
        assertThrows(IllegalArgumentException.class, () -> imports.finalizeImport("Test", List.of(item)));
        assertEquals(before, scalar("SELECT COUNT(*) FROM import_receipts"));
        item.setExpiryDate(null);
        imports.finalizeImport("Test", List.of(item));
        item.setExpiryDate(LocalDate.now().plusDays(10));
        imports.finalizeImport("Test", List.of(item));
        item.setExpiryDate(LocalDate.now().plusDays(7));
        imports.finalizeImport("Test", List.of(item));
        var inventory = new InventoryService().findItemsWithExpiry().stream()
                .filter(i -> i.getProductId() == 8102L).findFirst().orElseThrow();
        assertEquals(3, inventory.getQuantity());
        assertEquals(item.getExpiryDate(), inventory.getNearestExpiryDate());
        assertEquals("EXPIRING SOON", inventory.getExpiryStatus());
    }

    private static Product product(long id) throws Exception {
        sql("INSERT INTO products (id,name,sku,base_price,unit,active) VALUES (" + id + ",'Test food','TEST-" + id + "',10,'piece',true)");
        Product p = new Product();
        p.setId(id); p.setName("Test food"); p.setSku("TEST-" + id);
        p.setBasePrice(new BigDecimal("10.00")); p.setActive(true);
        return p;
    }

    private static void sql(String query) throws Exception {
        try (var c = ConnectionFactory.getConnection(); var s = c.createStatement()) { s.execute(query); }
    }

    private static int scalar(String query) throws Exception {
        try (var c = ConnectionFactory.getConnection(); var s = c.createStatement(); var rows = s.executeQuery(query)) {
            assertTrue(rows.next()); return rows.getInt(1);
        }
    }
}

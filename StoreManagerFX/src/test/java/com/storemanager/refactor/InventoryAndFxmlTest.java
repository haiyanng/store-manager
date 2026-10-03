package com.storemanager.refactor;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.database.DatabaseInitializer;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.dashboard.view.DashboardHomeController;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.inventory.view.InventoryListController;
import com.storemanager.domain.offline_export.dto.OfflineExportSelection;
import com.storemanager.domain.offline_export.service.OfflineExportJsonSerializer;
import com.storemanager.domain.offline_export.service.OfflineExportManifestGenerator;
import com.storemanager.domain.offline_export.service.OfflineExportSnapshotCollector;
import com.storemanager.domain.user.model.RoleType;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class InventoryAndFxmlTest {

    private static final List<String> errors = new CopyOnWriteArrayList<>();

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @BeforeClass
    public static void initializeIsolatedApplication() throws Exception {
        ConnectionFactory.setSettings(new DatabaseSettings() {
            @Override public String buildDatabaseUrl() {
                return "jdbc:h2:mem:refactor-fxml;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
            }
            @Override public String getUsername() { return "sa"; }
            @Override public String getPassword() { return ""; }
        });
        DatabaseInitializer.initialize();
        AppSession.setCurrentUser(RoleAndNavigationTest.user(1, RoleType.OWNER));
        System.setProperty("javafx.cachedir", "target/test-javafx-native");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> errors.add(error.toString()));
            started.countDown();
        });
        assertTrue(started.await(10, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void shutdown() throws Exception {
        Platform.exit();
        try (var connection = ConnectionFactory.getConnection(); var statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        } finally {
            AppSession.clear();
            ConnectionFactory.setSettings(null);
        }
    }

    @Test
    public void orderAndImportFxmlLoadWithPaymentAndExpiry() throws Exception {
        AppSession.setCurrentUser(RoleAndNavigationTest.user(1, RoleType.OWNER));
        for (String resource : List.of("/fxml/sale/sale.fxml", "/fxml/importing/import.fxml")) {
            FXMLLoader loader = onFx(() -> {
                FXMLLoader fxml = new FXMLLoader(getClass().getResource(resource));
                fxml.setControllerFactory(type -> {
                    if (type == com.storemanager.domain.sale.view.SaleController.class) {
                        return new com.storemanager.domain.sale.view.SaleController() {
                            @Override public void showError(String message) { errors.add(message); }
                        };
                    }
                    if (type == com.storemanager.domain.importing.view.ImportController.class) {
                        return new com.storemanager.domain.importing.view.ImportController() {
                            @Override public void showError(String message) { errors.add(message); }
                        };
                    }
                    try { return type.getDeclaredConstructor().newInstance(); }
                    catch (Exception e) { throw new IllegalStateException(e); }
                });
                fxml.load();
                return fxml;
            });
            waitForReady(loader);
            onFx(() -> {
                if (resource.contains("sale")) {
                    var amount = (javafx.scene.control.TextField) loader.getNamespace().get("amountReceivedField");
                    amount.setText("invalid");
                    assertEquals("Enter a valid amount", ((Label) loader.getNamespace().get("changeLabel")).getText());
                    amount.setText("1,000.00");
                    assertEquals("1,000.00", ((Label) loader.getNamespace().get("changeLabel")).getText());
                } else {
                    var picker = (javafx.scene.control.DatePicker) loader.getNamespace().get("expiryDatePicker");
                    picker.getEditor().setText("2026-02-30");
                    assertThrows(IllegalArgumentException.class, () -> com.storemanager.core.util.UiFeedback.readDate(picker));
                }
                return null;
            });
        }
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void inventoryLoadsAndRefreshesWithOnlyReadControls() throws Exception {
        FXMLLoader loader = onFx(() -> {
            FXMLLoader fxml = new FXMLLoader(getClass().getResource("/fxml/inventory/inventory-list.fxml"));
            fxml.setControllerFactory(type -> new InventoryListController() {
                @Override public void showError(String message) { errors.add(message); }
            });
            fxml.load();
            return fxml;
        });
        waitForReady(loader);
        onFx(() -> {
            assertNull(loader.getNamespace().get("adjustmentForm"));
            assertNull(loader.getNamespace().get("applyButton"));
            assertNull(loader.getNamespace().get("clearButton"));
            assertFalse(((TableView<?>) loader.getNamespace().get("inventoryTable")).isEditable());
            assertFalse(((TableView<?>) loader.getNamespace().get("transactionTable")).isEditable());
            var root = (javafx.scene.Parent) loader.getRoot();
            var buttons = root.lookupAll(".button");
            assertEquals(1, buttons.size());
            assertEquals("Refresh", ((Button) buttons.iterator().next()).getText());
            ((InventoryListController) loader.getController()).onRefresh();
            return null;
        });
        waitForReady(loader);
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void sharedStockWorkflowsStillWriteAndRollback() throws Exception {
        InventoryService service = new InventoryService();
        InventoryTransaction imported = transaction(InventoryTransactionType.IMPORT, 10);
        assertTrue(service.adjustStock(imported));
        assertEquals(10, quantity(service));
        AppSession.setCurrentUser(RoleAndNavigationTest.user(2, RoleType.STAFF));
        try (var connection = ConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            assertTrue(service.adjustStock(connection, transaction(InventoryTransactionType.SALE, 3)));
            connection.rollback();
        }
        assertEquals(10, quantity(service));
        try (var connection = ConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            assertTrue(service.adjustStock(connection, transaction(InventoryTransactionType.SALE, 3)));
            connection.commit();
        }
        assertEquals(7, quantity(service));
        AppSession.setCurrentUser(RoleAndNavigationTest.user(1, RoleType.OWNER));
        assertTrue(service.adjustStock(transaction(InventoryTransactionType.ADJUSTMENT, 3)));
        assertEquals(10, quantity(service));
    }

    @Test
    public void dashboardAndExportFxmlLoadWithoutRemovedBindings() throws Exception {
        FXMLLoader dashboard = onFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard/dashboard-home.fxml"));
            loader.setControllerFactory(type -> new DashboardHomeController() {
                @Override public void showError(String message) { errors.add(message); }
            });
            loader.load();
            return loader;
        });
        waitForReady(dashboard);
        onFx(() -> {
            assertNull(dashboard.getNamespace().get("payrollTable"));
            assertNull(dashboard.getNamespace().get("latestPayrollLabel"));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/system_tool/migration-export.fxml"));
            loader.load();
            assertNull(loader.getNamespace().get("payrollCheckBox"));
            assertNotNull(loader.getNamespace().get("productsCheckBox"));
            return null;
        });
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void exportWorksWithoutSalaryTablesOrPayrollFiles() throws Exception {
        try (var connection = ConnectionFactory.getConnection();
             var tables = connection.getMetaData().getTables(null, null, "payroll_records", null)) {
            assertFalse(tables.next());
        }
        var selection = OfflineExportSelection.all();
        assertTrue(selection.products() && selection.employees() && selection.sales()
                && selection.attendance() && selection.images() && selection.auditLogs());
        var bundle = new OfflineExportSnapshotCollector().collect(selection);
        var manifest = new OfflineExportManifestGenerator().generate("Test", "1", bundle);
        var destination = temporary.newFolder().toPath();
        new OfflineExportJsonSerializer().writePackage(destination, bundle, manifest);
        assertFalse(Files.exists(destination.resolve("payroll.json")));
        assertFalse(manifest.recordCounts().containsKey("payroll.json"));
        assertTrue(Files.exists(destination.resolve("products.json")));
        assertTrue(Files.exists(destination.resolve("attendance.json")));
        assertTrue(Files.exists(destination.resolve("sales.json")));
        assertTrue(Files.exists(destination.resolve("manifest.json")));
    }

    private static InventoryTransaction transaction(InventoryTransactionType type, int quantity) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductId(999L);
        transaction.setType(type);
        transaction.setQuantity(quantity);
        transaction.setReason("Workflow regression test");
        return transaction;
    }

    private static int quantity(InventoryService service) {
        return service.findAllItems().stream().filter(item -> item.getProductId() == 999L)
                .findFirst().orElseThrow().getQuantity();
    }

    private static void waitForReady(FXMLLoader loader) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (onFx(() -> "Ready".equals(((Label) loader.getNamespace().get("statusLabel")).getText()))) {
                return;
            }
            assertTrue(errors.toString(), errors.isEmpty());
            Thread.sleep(25);
        }
        fail("FXML data loading did not complete");
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}

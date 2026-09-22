package com.storemanager.core.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import com.storemanager.core.security.PasswordHasher;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;



public class DatabaseInitializer {

    public static void initialize() {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(100) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        role VARCHAR(50) NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

            initializeBusinessTables(connection, statement);
            new UserRepository().migrateLegacyRoles();
            createDefaultAdmin();
            System.out.println("Database initialized successfully");

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Database initialization failed",
                    e
            );
        }
    }

    private static void initializeBusinessTables(
            Connection connection,
            Statement statement
    ) throws Exception {

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS employees (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    full_name VARCHAR(150) NOT NULL,
                    phone VARCHAR(40),
                    address VARCHAR(255),
                    position VARCHAR(100) NOT NULL,
                    user_id BIGINT NULL,
                    image_path VARCHAR(255) NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS categories (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(150) NOT NULL,
                    image_path VARCHAR(255) NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS products (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(180) NOT NULL,
                    sku VARCHAR(80) NOT NULL,
                    barcode VARCHAR(80),
                    category_id BIGINT NULL,
                    base_price DECIMAL(18, 2) NOT NULL DEFAULT 0,
                    unit VARCHAR(40) NOT NULL,
                    image_path VARCHAR(255) NULL,
                    short_description VARCHAR(255) NULL,
                    full_description TEXT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
        addColumnIfMissing(
                connection,
                "products",
                "short_description",
                "ALTER TABLE products ADD COLUMN short_description VARCHAR(255) NULL"
        );
        addColumnIfMissing(
                connection,
                "products",
                "full_description",
                "ALTER TABLE products ADD COLUMN full_description TEXT NULL"
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS inventory_items (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    product_id BIGINT NOT NULL UNIQUE,
                    quantity INT NOT NULL DEFAULT 0,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS inventory_transactions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    product_id BIGINT NOT NULL,
                    type VARCHAR(40) NOT NULL,
                    quantity INT NOT NULL,
                    reason VARCHAR(255),
                    created_by_user_id BIGINT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

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

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS import_receipts (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    supplier_name VARCHAR(180) NOT NULL,
                    total_cost DECIMAL(18, 2) NOT NULL DEFAULT 0,
                    created_by_user_id BIGINT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS import_items (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    import_receipt_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    unit_cost DECIMAL(18, 2) NOT NULL,
                    subtotal DECIMAL(18, 2) NOT NULL
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS customers (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    email VARCHAR(180) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    full_name VARCHAR(180) NOT NULL,
                    phone VARCHAR(40),
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS customer_addresses (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    customer_id BIGINT NOT NULL,
                    recipient_name VARCHAR(180),
                    phone VARCHAR(40),
                    line1 VARCHAR(255),
                    city VARCHAR(120),
                    country VARCHAR(120),
                    is_default BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS carts (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    customer_id BIGINT NOT NULL UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS cart_items (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    cart_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_cart_product (cart_id, product_id)
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS orders (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    customer_id BIGINT NOT NULL,
                    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
                    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
                    recipient_name VARCHAR(180),
                    phone VARCHAR(40),
                    shipping_address VARCHAR(500),
                    payment_method VARCHAR(80),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS order_items (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    product_name VARCHAR(180) NOT NULL,
                    quantity INT NOT NULL,
                    unit_price DECIMAL(18, 2) NOT NULL,
                    subtotal DECIMAL(18, 2) NOT NULL
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS payment_methods (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    customer_id BIGINT NOT NULL,
                    label VARCHAR(120) NOT NULL,
                    provider VARCHAR(80) NOT NULL,
                    last_four VARCHAR(8),
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS online_order_status_history (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    action VARCHAR(40) NOT NULL,
                    old_status VARCHAR(40) NOT NULL,
                    new_status VARCHAR(40) NOT NULL,
                    note VARCHAR(1000),
                    created_by_user_id BIGINT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS branches (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(160) NOT NULL,
                    code VARCHAR(80),
                    address VARCHAR(255),
                    phone VARCHAR(40),
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS employee_branch_assignments (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    employee_id BIGINT NOT NULL,
                    branch_id BIGINT NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    assigned_by_user_id BIGINT
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS attendance_sessions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    employee_id BIGINT NOT NULL,
                    branch_id BIGINT NULL,
                    check_in_time TIMESTAMP NOT NULL,
                    check_out_time TIMESTAMP NULL,
                    worked_hours DECIMAL(10, 2) NULL,
                    created_by_user_id BIGINT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notifications (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    title VARCHAR(160) NOT NULL,
                    content TEXT,
                    type VARCHAR(40) NOT NULL,
                    is_read BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    sender_user_id BIGINT NOT NULL,
                    receiver_user_id BIGINT NOT NULL,
                    content TEXT NOT NULL,
                    is_read BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        statement.execute(
                """
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT,
                    actor_username VARCHAR(100),
                    module VARCHAR(80),
                    action VARCHAR(80) NOT NULL,
                    entity_type VARCHAR(120) NOT NULL,
                    entity_id BIGINT,
                    success BOOLEAN NOT NULL DEFAULT TRUE,
                    reason VARCHAR(500),
                    details_json TEXT,
                    details TEXT,
                    branch_id BIGINT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        addColumnIfMissing(connection, "employees", "user_id", "ALTER TABLE employees ADD COLUMN user_id BIGINT NULL");
        addColumnIfMissing(connection, "employees", "image_path", "ALTER TABLE employees ADD COLUMN image_path VARCHAR(255) NULL");
        addColumnIfMissing(connection, "categories", "image_path", "ALTER TABLE categories ADD COLUMN image_path VARCHAR(255) NULL");
        addColumnIfMissing(connection, "products", "image_path", "ALTER TABLE products ADD COLUMN image_path VARCHAR(255) NULL");
        addColumnIfMissing(connection, "products", "short_description", "ALTER TABLE products ADD COLUMN short_description VARCHAR(255) NULL");
        addColumnIfMissing(connection, "products", "full_description", "ALTER TABLE products ADD COLUMN full_description TEXT NULL");
        addColumnIfMissing(connection, "orders", "updated_at", "ALTER TABLE orders ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        addColumnIfMissing(connection, "employee_branch_assignments", "assigned_by_user_id", "ALTER TABLE employee_branch_assignments ADD COLUMN assigned_by_user_id BIGINT");
        addColumnIfMissing(connection, "attendance_sessions", "branch_id", "ALTER TABLE attendance_sessions ADD COLUMN branch_id BIGINT NULL");
        addColumnIfMissing(connection, "audit_logs", "actor_username", "ALTER TABLE audit_logs ADD COLUMN actor_username VARCHAR(100)");
        addColumnIfMissing(connection, "audit_logs", "module", "ALTER TABLE audit_logs ADD COLUMN module VARCHAR(80)");
        addColumnIfMissing(connection, "audit_logs", "success", "ALTER TABLE audit_logs ADD COLUMN success BOOLEAN NOT NULL DEFAULT TRUE");
        addColumnIfMissing(connection, "audit_logs", "reason", "ALTER TABLE audit_logs ADD COLUMN reason VARCHAR(500)");
        addColumnIfMissing(connection, "audit_logs", "details_json", "ALTER TABLE audit_logs ADD COLUMN details_json TEXT");
    }

    private static void addColumnIfMissing(
            Connection connection,
            String table,
            String column,
            String alterSql
    ) throws Exception {

        DatabaseMetaData metaData =
                connection.getMetaData();

        try (
                ResultSet columns =
                        metaData.getColumns(
                                null,
                                null,
                                table,
                                column
                        )
        ) {
            if (columns.next()) {
                return;
            }
        }

        try (
                Statement statement =
                        connection.createStatement()
        ) {
            statement.execute(alterSql);
        }
    }

    private static void createDefaultAdmin() {

        UserRepository repository =
                new UserRepository();

        boolean exists =
                repository
                        .findByUsername("admin")
                        .isPresent();

        if (exists) {
            return;
        }

        User admin =
                new User();

        admin.setUsername("admin");

        admin.setPassword(
                PasswordHasher.hash("123456")
        );

        admin.setRole(
                RoleType.OWNER
        );

        admin.setActive(true);

        repository.save(admin);

        System.out.println(
                "Default admin created"
        );
    }
}

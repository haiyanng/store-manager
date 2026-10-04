package com.storemanager.domain.product.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.product.model.Product;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    public List<Product> findAll() {

        return findByActiveState(true);
    }

    public List<Product> findAllIncludingInactive() {

        return findByActiveState(null);
    }

    private List<Product> findByActiveState(
            Boolean active
    ) {

        List<Product> products =
                new ArrayList<>();

        String sql =
                active == null
                        ? """
                          SELECT *
                          FROM products
                          ORDER BY id
                          """
                        : """
                          SELECT *
                          FROM products
                          WHERE active = ?
                          ORDER BY id
                          """;

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            if (active != null) {
                statement.setBoolean(1, active);
            }

            ResultSet resultSet =
                    statement.executeQuery();

            while (resultSet.next()) {
                products.add(
                        mapProduct(resultSet)
                );
            }

            return products;

        } catch (Exception e) {

            throw new IllegalStateException("Unable to load products. Check the database connection and try again.", e);
        }
    }

    public Product findById(Long id) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM products WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapProduct(result) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load product. Check the database connection and try again.", e);
        }
    }

    public boolean existsSku(String sku, Long excludedProductId) {
        String sql = "SELECT 1 FROM products WHERE UPPER(TRIM(sku)) = UPPER(TRIM(?))"
                + (excludedProductId == null ? "" : " AND id <> ?") + " LIMIT 1";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sku);
            if (excludedProductId != null) statement.setLong(2, excludedProductId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to check SKU availability. Check the database connection and try again.", e);
        }
    }

    public boolean save(
            Product product
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO products (
                                    name,
                                    sku,
                                    category_id,
                                    base_price,
                                    unit,
                                    image_path,
                                    active
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?)
                                """, Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            fillStatement(statement, product);

            boolean saved = statement.executeUpdate() > 0;
            if (saved) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) product.setId(keys.getLong(1));
                }
            }
            return saved;

        } catch (Exception e) {
            throw saveFailure(product, e);
        }
    }

    public boolean update(
            Product product
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE products
                                SET name = ?,
                                    sku = ?,
                                    category_id = ?,
                                    base_price = ?,
                                    unit = ?,
                                    image_path = ?,
                                    active = ?
                                WHERE id = ?
                                """
                        )
        ) {

            fillStatement(statement, product);

            statement.setLong(
                    8,
                    product.getId()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            throw saveFailure(product, e);
        }
    }

    public boolean delete(
            Product product
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE products
                                SET active = FALSE
                                WHERE id = ?
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    product.getId()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public static void initializeSchema(Connection connection) {

        try (
                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS products (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(180) NOT NULL,
                        sku VARCHAR(80) NOT NULL,
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

            addImagePathColumnIfMissing(connection);
            addColumnIfMissing(
                    connection,
                    "short_description",
                    "ALTER TABLE products ADD COLUMN short_description VARCHAR(255) NULL"
            );
            addColumnIfMissing(
                    connection,
                    "full_description",
                    "ALTER TABLE products ADD COLUMN full_description TEXT NULL"
            );
            initializeSkuConstraint(connection);

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Product table initialization failed: " + e.getMessage(),
                    e
            );
        }
    }

    private static void addImagePathColumnIfMissing(
            Connection connection
    ) throws Exception {

        DatabaseMetaData metaData =
                connection.getMetaData();

        try (
                ResultSet columns =
                        metaData.getColumns(
                                null,
                                null,
                                "products",
                                "image_path"
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

            statement.execute(
                    """
                    ALTER TABLE products
                    ADD COLUMN image_path VARCHAR(255) NULL
                    """
            );
        }
    }

    private static void addColumnIfMissing(
            Connection connection,
            String columnName,
            String alterSql
    ) throws Exception {

        DatabaseMetaData metaData =
                connection.getMetaData();

        try (
                ResultSet columns =
                        metaData.getColumns(
                                null,
                                null,
                                "products",
                                columnName
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

    private static void initializeSkuConstraint(Connection connection) throws Exception {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(
                connection.getCatalog(), null, "products", true, false)) {
            while (indexes.next()) {
                if ("uk_products_sku_normalized".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return;
            }
        }

        List<String> duplicates = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT UPPER(TRIM(sku)) AS normalized_sku
                     FROM products
                     GROUP BY UPPER(TRIM(sku))
                     HAVING COUNT(*) > 1
                     ORDER BY normalized_sku
                     """)) {
            while (rows.next() && duplicates.size() < 5) {
                String sku = rows.getString("normalized_sku");
                List<Long> ids = new ArrayList<>();
                try (PreparedStatement details = connection.prepareStatement(
                        "SELECT id FROM products WHERE UPPER(TRIM(sku)) = ? ORDER BY id")) {
                    details.setString(1, sku);
                    try (ResultSet products = details.executeQuery()) {
                        while (products.next()) ids.add(products.getLong("id"));
                    }
                }
                duplicates.add("'" + sku + "' (product IDs " + ids + ")");
            }
        }
        if (!duplicates.isEmpty()) {
            throw new IllegalStateException("Cannot enforce unique SKUs because existing products share "
                    + String.join(", ", duplicates)
                    + ". Assign a distinct SKU to each listed product in the database, then restart. No product data was changed.");
        }

        // H2 tests use the same expression; MySQL/MariaDB store the generated value for the unique index.
        boolean h2 = "H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        addColumnIfMissing(connection, "sku_normalized",
                "ALTER TABLE products ADD COLUMN sku_normalized VARCHAR(80) GENERATED ALWAYS AS (UPPER(TRIM(sku)))"
                        + (h2 ? "" : " STORED"));
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE UNIQUE INDEX uk_products_sku_normalized ON products (sku_normalized)");
        }
    }

    private static RuntimeException saveFailure(Product product, Exception error) {
        if (error instanceof SQLException sql
                && (sql.getErrorCode() == 1062 || "23505".equals(sql.getSQLState()))) {
            return new IllegalArgumentException("SKU '" + product.getSku()
                    + "' is already assigned to another product. Use a different SKU.", error);
        }
        return new IllegalStateException("Unable to save product changes. Check the database connection and try again.", error);
    }

    private void fillStatement(
            PreparedStatement statement,
            Product product
    ) throws Exception {

        statement.setString(1, product.getName());
        statement.setString(2, product.getSku());

        if (product.getCategoryId() == null) {
            statement.setObject(3, null);
        } else {
            statement.setLong(3, product.getCategoryId());
        }

        statement.setBigDecimal(4, product.getBasePrice());
        statement.setString(5, product.getUnit());
        statement.setString(6, product.getImagePath());
        statement.setBoolean(7, product.isActive());
    }

    private Product mapProduct(
            ResultSet resultSet
    ) throws Exception {

        Product product =
                new Product();

        product.setId(resultSet.getLong("id"));
        product.setName(resultSet.getString("name"));
        product.setSku(resultSet.getString("sku"));

        long categoryId =
                resultSet.getLong("category_id");

        if (resultSet.wasNull()) {
            product.setCategoryId(null);
        } else {
            product.setCategoryId(categoryId);
        }

        product.setBasePrice(resultSet.getBigDecimal("base_price"));
        product.setUnit(resultSet.getString("unit"));
        product.setImagePath(resultSet.getString("image_path"));
        product.setActive(resultSet.getBoolean("active"));

        return product;
    }
}

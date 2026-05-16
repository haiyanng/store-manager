package com.storemanager.domain.product.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.product.model.Product;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    public ProductRepository() {

        initializeTable();
    }

    public List<Product> findAll() {

        List<Product> products =
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
                                FROM products
                                ORDER BY id
                                """
                        )
        ) {

            while (resultSet.next()) {
                products.add(
                        mapProduct(resultSet)
                );
            }

            return products;

        } catch (Exception e) {

            e.printStackTrace();

            return products;
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
                                    barcode,
                                    category_id,
                                    base_price,
                                    unit,
                                    image_path,
                                    active
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                """
                        )
        ) {

            fillStatement(statement, product);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
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
                                    barcode = ?,
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
                    9,
                    product.getId()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
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
                                DELETE FROM products
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

    private void initializeTable() {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

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
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

            addImagePathColumnIfMissing(connection);

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Product table initialization failed",
                    e
            );
        }
    }

    private void addImagePathColumnIfMissing(
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

    private void fillStatement(
            PreparedStatement statement,
            Product product
    ) throws Exception {

        statement.setString(1, product.getName());
        statement.setString(2, product.getSku());
        statement.setString(3, product.getBarcode());

        if (product.getCategoryId() == null) {
            statement.setObject(4, null);
        } else {
            statement.setLong(4, product.getCategoryId());
        }

        statement.setBigDecimal(5, product.getBasePrice());
        statement.setString(6, product.getUnit());
        statement.setString(7, product.getImagePath());
        statement.setBoolean(8, product.isActive());
    }

    private Product mapProduct(
            ResultSet resultSet
    ) throws Exception {

        Product product =
                new Product();

        product.setId(resultSet.getLong("id"));
        product.setName(resultSet.getString("name"));
        product.setSku(resultSet.getString("sku"));
        product.setBarcode(resultSet.getString("barcode"));

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

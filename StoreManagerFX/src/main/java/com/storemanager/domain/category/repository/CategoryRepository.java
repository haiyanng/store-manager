package com.storemanager.domain.category.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.category.model.Category;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    public CategoryRepository() {

        initializeTable();
    }

    public List<Category> findAll() {

        List<Category> categories =
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
                                FROM categories
                                ORDER BY id
                                """
                        )
        ) {

            while (resultSet.next()) {
                categories.add(
                        mapCategory(resultSet)
                );
            }

            return categories;

        } catch (Exception e) {

            e.printStackTrace();

            return categories;
        }
    }

    public boolean save(
            Category category
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO categories (
                                    name,
                                    image_path,
                                    active
                                )
                                VALUES (?, ?, ?)
                                """
                        )
        ) {

            fillStatement(statement, category);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean update(
            Category category
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE categories
                                SET name = ?,
                                    image_path = ?,
                                    active = ?
                                WHERE id = ?
                                """
                        )
        ) {

            fillStatement(statement, category);

            statement.setLong(
                    4,
                    category.getId()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean delete(
            Category category
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                DELETE FROM categories
                                WHERE id = ?
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    category.getId()
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
                    CREATE TABLE IF NOT EXISTS categories (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(150) NOT NULL,
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
                    "Category table initialization failed",
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
                                "categories",
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
                    ALTER TABLE categories
                    ADD COLUMN image_path VARCHAR(255) NULL
                    """
            );
        }
    }

    private void fillStatement(
            PreparedStatement statement,
            Category category
    ) throws Exception {

        statement.setString(
                1,
                category.getName()
        );

        statement.setString(
                2,
                category.getImagePath()
        );

        statement.setBoolean(
                3,
                category.isActive()
        );
    }

    private Category mapCategory(
            ResultSet resultSet
    ) throws Exception {

        Category category =
                new Category();

        category.setId(
                resultSet.getLong("id")
        );

        category.setName(
                resultSet.getString("name")
        );

        category.setImagePath(
                resultSet.getString("image_path")
        );

        category.setActive(
                resultSet.getBoolean("active")
        );

        return category;
    }
}

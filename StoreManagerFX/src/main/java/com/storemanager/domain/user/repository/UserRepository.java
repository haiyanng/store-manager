package com.storemanager.domain.user.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    public Optional<User> findByUsername(
            String username
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM users
                                WHERE username = ?
                                """
                        )

        ) {

            statement.setString(
                    1,
                    username
            );

            ResultSet resultSet =
                    statement.executeQuery();

            if (resultSet.next()) {

                return Optional.of(
                        mapUser(resultSet)
                );
            }

            return Optional.empty();

        } catch (Exception e) {

            e.printStackTrace();

            return Optional.empty();
        }
    }

    public Optional<User> findById(
            Long id
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM users
                                WHERE id = ?
                                """
                        )

        ) {

            statement.setLong(
                    1,
                    id
            );

            ResultSet resultSet =
                    statement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(
                        mapUser(resultSet)
                );
            }

            return Optional.empty();

        } catch (Exception e) {

            e.printStackTrace();

            return Optional.empty();
        }
    }

    public void save(
            User user
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO users (
                                    username,
                                    password,
                                    role,
                                    active
                                )
                                VALUES (?, ?, ?, ?)
                                """
                        )

        ) {

            statement.setString(
                    1,
                    user.getUsername()
            );

            statement.setString(
                    2,
                    user.getPassword()
            );

            statement.setString(
                    3,
                    user.getRole().name()
            );

            statement.setBoolean(
                    4,
                    user.isActive()
            );

            statement.executeUpdate();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public boolean updateUser(
            User user
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE users
                                SET username = ?,
                                    password = ?,
                                    role = ?,
                                    active = ?
                                WHERE id = ?
                                """
                        )

        ) {

            statement.setString(
                    1,
                    user.getUsername()
            );

            statement.setString(
                    2,
                    user.getPassword()
            );

            statement.setString(
                    3,
                    user.getRole().name()
            );

            statement.setBoolean(
                    4,
                    user.isActive()
            );

            statement.setLong(
                    5,
                    user.getId()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean deleteUser(
            User user
    ) {

        try (

                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                DELETE FROM users
                                WHERE id = ?
                                """
                        )

        ) {

            statement.setLong(
                    1,
                    user.getId()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public List<User> findAll() {

        List<User> users =
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
                                FROM users
                                ORDER BY id
                                """
                        )

        ) {

            while (resultSet.next()) {
                users.add(
                        mapUser(resultSet)
                );
            }

            return users;

        } catch (Exception e) {

            e.printStackTrace();

            return users;
        }
    }

    private User mapUser(
            ResultSet resultSet
    ) throws Exception {

        User user =
                new User();

        user.setId(
                resultSet.getLong("id")
        );

        user.setUsername(
                resultSet.getString("username")
        );

        user.setPassword(
                resultSet.getString("password")
        );

        user.setRole(
                RoleType.fromDatabaseValue(
                        resultSet.getString("role")
                )
        );

        user.setActive(
                resultSet.getBoolean("active")
        );

        Timestamp createdAt =
                resultSet.getTimestamp("created_at");

        if (createdAt != null) {
            user.setCreatedAt(
                    createdAt.toLocalDateTime()
            );
        }

        return user;
    }
}

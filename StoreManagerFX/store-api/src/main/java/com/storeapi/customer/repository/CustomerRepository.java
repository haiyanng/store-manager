package com.storeapi.customer.repository;

import com.storeapi.customer.model.Customer;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CustomerRepository {
    private final JdbcClient jdbc;

    public CustomerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Customer> findByEmail(String email) {
        return jdbc.sql("SELECT * FROM customers WHERE email = :email")
                .param("email", email)
                .query((rs, rowNum) -> map(rs))
                .optional();
    }

    public Optional<Customer> findById(Long id) {
        return jdbc.sql("SELECT * FROM customers WHERE id = :id")
                .param("id", id)
                .query((rs, rowNum) -> map(rs))
                .optional();
    }

    public Long create(String email, String passwordHash, String fullName, String phone) {
        jdbc.sql("""
                INSERT INTO customers (email, password_hash, full_name, phone, active)
                VALUES (:email, :passwordHash, :fullName, :phone, TRUE)
                """)
                .param("email", email)
                .param("passwordHash", passwordHash)
                .param("fullName", fullName)
                .param("phone", phone)
                .update();
        return jdbc.sql("SELECT id FROM customers WHERE email = :email")
                .param("email", email)
                .query(Long.class)
                .single();
    }

    public void updateProfile(Long id, String fullName, String phone) {
        jdbc.sql("UPDATE customers SET full_name = :fullName, phone = :phone WHERE id = :id")
                .param("fullName", fullName)
                .param("phone", phone)
                .param("id", id)
                .update();
    }

    public void updatePassword(Long id, String passwordHash) {
        jdbc.sql("UPDATE customers SET password_hash = :passwordHash WHERE id = :id")
                .param("passwordHash", passwordHash)
                .param("id", id)
                .update();
    }

    private Customer map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Customer(rs.getLong("id"), rs.getString("email"), rs.getString("password_hash"),
                rs.getString("full_name"), rs.getString("phone"), rs.getBoolean("active"));
    }
}

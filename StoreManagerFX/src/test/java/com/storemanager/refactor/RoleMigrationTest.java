package com.storemanager.refactor;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.security.PasswordHasher;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.auth.service.AuthService;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.repository.UserRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.Assert.*;

public class RoleMigrationTest {

    private String databaseUrl;

    @Before
    public void createIsolatedDatabase() throws Exception {
        databaseUrl = "jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        ConnectionFactory.setSettings(new DatabaseSettings() {
            @Override public String buildDatabaseUrl() { return databaseUrl; }
            @Override public String getUsername() { return "sa"; }
            @Override public String getPassword() { return ""; }
        });
        try (var connection = ConnectionFactory.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100), password VARCHAR(255), role VARCHAR(50), active BOOLEAN, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            try (var insert = connection.prepareStatement("INSERT INTO users(username, password, role, active) VALUES (?, ?, ?, ?)")) {
                for (String role : new String[]{"USER", " user ", "CUSTOMER", "DEVELOPER", "ADMIN", "MANAGER", "EMPLOYEE"}) {
                    insert.setString(1, "account-" + role.trim());
                    insert.setString(2, PasswordHasher.hash("test-password"));
                    insert.setString(3, role);
                    insert.setBoolean(4, !role.equals(" user "));
                    insert.executeUpdate();
                }
            }
        }
    }

    @After
    public void closeDatabase() throws Exception {
        AppSession.clear();
        try (var connection = DriverManager.getConnection(databaseUrl, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        } finally {
            ConnectionFactory.setSettings(null);
        }
    }

    @Test
    public void migrationIsIdempotentAndPreservesAccountIdentity() throws Exception {
        UserRepository repository = new UserRepository();
        repository.migrateLegacyRoles();
        repository.migrateLegacyRoles();
        var users = repository.findAll();
        assertEquals(7, users.size());
        assertEquals(RoleType.CUSTOMER, users.get(0).getRole());
        assertEquals(RoleType.CUSTOMER, users.get(1).getRole());
        assertFalse(users.get(1).isActive());
        assertEquals(RoleType.OWNER, users.get(3).getRole());
        assertEquals(RoleType.OWNER, users.get(4).getRole());
        assertEquals(RoleType.MANAGER, users.get(5).getRole());
        assertEquals(RoleType.STAFF, users.get(6).getRole());
        for (int index = 0; index < users.size(); index++) {
            assertEquals(Long.valueOf(index + 1), users.get(index).getId());
            assertEquals(PasswordHasher.hash("test-password"), users.get(index).getPassword());
        }
        try (var connection = ConnectionFactory.getConnection(); var statement = connection.createStatement();
             var results = statement.executeQuery("SELECT COUNT(*) FROM users WHERE UPPER(TRIM(role)) IN ('USER','DEVELOPER','ADMIN')")) {
            assertTrue(results.next());
            assertEquals(0, results.getInt(1));
        }
    }

    @Test
    public void unmigratedUserCanAuthenticateAndSaveAsCustomer() {
        AuthService auth = new AuthService();
        assertTrue(auth.login("account-USER", "test-password"));
        var loggedIn = AppSession.getCurrentUser();
        assertEquals(RoleType.CUSTOMER, loggedIn.getRole());
        assertEquals(Long.valueOf(1), loggedIn.getId());
        UserRepository repository = new UserRepository();
        assertTrue(repository.updateUser(loggedIn));
        assertEquals(RoleType.CUSTOMER, repository.findById(1L).orElseThrow().getRole());
        auth.logout();
        assertFalse(AppSession.isLoggedIn());
        assertFalse(auth.login("account-user", "test-password"));
        assertFalse(auth.login("account-USER", "wrong-password"));
    }
}

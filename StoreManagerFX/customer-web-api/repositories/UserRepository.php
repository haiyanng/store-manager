<?php

declare(strict_types=1);

final class UserRepository
{
    public function __construct(private readonly PDO $connection)
    {
    }

    public function create(array $data): int
    {
        $sql = 'INSERT INTO users (role_id, name, email, password_hash, active)
                VALUES (:role_id, :name, :email, :password_hash, :active)';
        $stmt = $this->connection->prepare($sql);
        $stmt->execute([
            'role_id' => $data['role_id'],
            'name' => $data['name'],
            'email' => $data['email'],
            'password_hash' => $data['password_hash'],
            'active' => !empty($data['active']) ? 1 : 0,
        ]);

        return (int) $this->connection->lastInsertId();
    }

    public function findByEmail(string $email): ?array
    {
        $stmt = $this->connection->prepare('SELECT id, role_id, name, email, password_hash, active, created_at, updated_at FROM users WHERE email = :email LIMIT 1');
        $stmt->execute(['email' => $email]);
        $row = $stmt->fetch();

        return $row === false ? null : $row;
    }

    public function findByEmailWithRole(string $email): ?array
    {
        $sql = 'SELECT u.id, u.role_id, u.name, u.email, u.password_hash, u.active, u.created_at, u.updated_at,
                       r.id AS role_ref_id, r.name AS role_name, r.display_name AS role_display_name, r.created_at AS role_created_at
                FROM users u
                INNER JOIN roles r ON r.id = u.role_id
                WHERE u.email = :email
                LIMIT 1';
        $stmt = $this->connection->prepare($sql);
        $stmt->execute(['email' => $email]);
        $row = $stmt->fetch();

        return $row === false ? null : $row;
    }

    public function findByIdWithRole(int $id): ?array
    {
        $sql = 'SELECT u.id, u.role_id, u.name, u.email, u.password_hash, u.active, u.created_at, u.updated_at,
                       r.id AS role_ref_id, r.name AS role_name, r.display_name AS role_display_name, r.created_at AS role_created_at
                FROM users u
                INNER JOIN roles r ON r.id = u.role_id
                WHERE u.id = :id
                LIMIT 1';
        $stmt = $this->connection->prepare($sql);
        $stmt->execute(['id' => $id]);
        $row = $stmt->fetch();

        return $row === false ? null : $row;
    }

    public function findActiveUserByTokenHash(string $tokenHash): ?array
    {
        $sql = 'SELECT t.id AS token_id, t.token_hash, t.expires_at, t.revoked_at,
                       u.id, u.role_id, u.name, u.email, u.password_hash, u.active, u.created_at, u.updated_at,
                       r.id AS role_ref_id, r.name AS role_name, r.display_name AS role_display_name, r.created_at AS role_created_at
                FROM auth_tokens t
                INNER JOIN users u ON u.id = t.user_id
                INNER JOIN roles r ON r.id = u.role_id
                WHERE t.token_hash = :token_hash
                LIMIT 1';
        $stmt = $this->connection->prepare($sql);
        $stmt->execute(['token_hash' => $tokenHash]);
        $row = $stmt->fetch();

        return $row === false ? null : $row;
    }

    public function createAuthToken(int $userId, string $tokenHash, string $expiresAt): int
    {
        $stmt = $this->connection->prepare('
            INSERT INTO auth_tokens (user_id, token_hash, expires_at)
            VALUES (:user_id, :token_hash, :expires_at)
        ');
        $stmt->execute([
            'user_id' => $userId,
            'token_hash' => $tokenHash,
            'expires_at' => $expiresAt,
        ]);

        return (int) $this->connection->lastInsertId();
    }

    public function revokeToken(string $tokenHash): bool
    {
        $stmt = $this->connection->prepare('
            UPDATE auth_tokens
            SET revoked_at = NOW()
            WHERE token_hash = :token_hash AND revoked_at IS NULL
        ');
        $stmt->execute(['token_hash' => $tokenHash]);

        return $stmt->rowCount() > 0;
    }

    public function emailExists(string $email): bool
    {
        $stmt = $this->connection->prepare('SELECT 1 FROM users WHERE email = :email LIMIT 1');
        $stmt->execute(['email' => $email]);
        return (bool) $stmt->fetchColumn();
    }

    public function findAllWithRole(): array
    {
        $sql = 'SELECT u.id, u.role_id, u.name, u.email, u.active, u.created_at, u.updated_at,
                       r.id AS role_ref_id, r.name AS role_name, r.display_name AS role_display_name, r.created_at AS role_created_at
                FROM users u
                INNER JOIN roles r ON r.id = u.role_id
                ORDER BY u.id ASC';
        $stmt = $this->connection->prepare($sql);
        $stmt->execute();

        return $stmt->fetchAll();
    }

    public function update(int $id, array $data): bool
    {
        $sets = [
            'role_id = :role_id',
            'name = :name',
            'email = :email',
            'active = :active',
        ];
        $params = [
            'id' => $id,
            'role_id' => $data['role_id'],
            'name' => $data['name'],
            'email' => $data['email'],
            'active' => !empty($data['active']) ? 1 : 0,
        ];

        if (!empty($data['password_hash'])) {
            $sets[] = 'password_hash = :password_hash';
            $params['password_hash'] = $data['password_hash'];
        }

        $sql = 'UPDATE users SET ' . implode(', ', $sets) . ' WHERE id = :id';
        $stmt = $this->connection->prepare($sql);
        $stmt->execute($params);

        return $stmt->rowCount() > 0;
    }

    public function delete(int $id): bool
    {
        $stmt = $this->connection->prepare('DELETE FROM users WHERE id = :id');
        $stmt->execute(['id' => $id]);

        return $stmt->rowCount() > 0;
    }

    public function emailExistsExcept(string $email, int $exceptId): bool
    {
        $stmt = $this->connection->prepare('SELECT 1 FROM users WHERE email = :email AND id <> :id LIMIT 1');
        $stmt->execute([
            'email' => $email,
            'id' => $exceptId,
        ]);

        return (bool) $stmt->fetchColumn();
    }

    public function activeUserCountForRoleName(string $roleName): int
    {
        $sql = 'SELECT COUNT(*)
                FROM users u
                INNER JOIN roles r ON r.id = u.role_id
                WHERE r.name = :role_name AND u.active = 1';
        $stmt = $this->connection->prepare($sql);
        $stmt->execute(['role_name' => $roleName]);

        return (int) $stmt->fetchColumn();
    }
}

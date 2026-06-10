<?php

declare(strict_types=1);

final class RoleRepository
{
    public function __construct(private readonly PDO $connection)
    {
    }

    public function findAll(): array
    {
        $stmt = $this->connection->prepare('SELECT id, name, display_name, created_at FROM roles ORDER BY id ASC');
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public function findByName(string $name): ?array
    {
        $stmt = $this->connection->prepare('SELECT id, name, display_name, created_at FROM roles WHERE name = :name LIMIT 1');
        $stmt->execute(['name' => $name]);
        $row = $stmt->fetch();

        return $row === false ? null : $row;
    }

    public function findById(int $id): ?array
    {
        $stmt = $this->connection->prepare('SELECT id, name, display_name, created_at FROM roles WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $row = $stmt->fetch();

        return $row === false ? null : $row;
    }

    public function create(array $data): int
    {
        $stmt = $this->connection->prepare('
            INSERT INTO roles (name, display_name)
            VALUES (:name, :display_name)
        ');
        $stmt->execute([
            'name' => $data['name'],
            'display_name' => $data['display_name'],
        ]);

        return (int) $this->connection->lastInsertId();
    }

    public function update(int $id, array $data): bool
    {
        $stmt = $this->connection->prepare('
            UPDATE roles
            SET name = :name, display_name = :display_name
            WHERE id = :id
        ');
        $stmt->execute([
            'id' => $id,
            'name' => $data['name'],
            'display_name' => $data['display_name'],
        ]);

        return $stmt->rowCount() > 0;
    }

    public function delete(int $id): bool
    {
        $stmt = $this->connection->prepare('DELETE FROM roles WHERE id = :id');
        $stmt->execute(['id' => $id]);

        return $stmt->rowCount() > 0;
    }

    public function nameExists(string $name, ?int $exceptId = null): bool
    {
        if ($exceptId === null) {
            $stmt = $this->connection->prepare('SELECT 1 FROM roles WHERE name = :name LIMIT 1');
            $stmt->execute(['name' => $name]);
            return (bool) $stmt->fetchColumn();
        }

        $stmt = $this->connection->prepare('SELECT 1 FROM roles WHERE name = :name AND id <> :id LIMIT 1');
        $stmt->execute([
            'name' => $name,
            'id' => $exceptId,
        ]);

        return (bool) $stmt->fetchColumn();
    }

    public function userCount(int $id): int
    {
        $stmt = $this->connection->prepare('SELECT COUNT(*) FROM users WHERE role_id = :id');
        $stmt->execute(['id' => $id]);

        return (int) $stmt->fetchColumn();
    }
}

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
}

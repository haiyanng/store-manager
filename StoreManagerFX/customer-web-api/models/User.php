<?php

declare(strict_types=1);

final class User
{
    public function __construct(
        public readonly ?int $id,
        public readonly int $roleId,
        public readonly string $name,
        public readonly string $email,
        public readonly ?string $passwordHash = null,
        public readonly bool $active = true,
        public readonly ?string $createdAt = null,
        public readonly ?string $updatedAt = null,
        public readonly ?Role $role = null
    ) {
    }

    public static function fromArray(array $row): self
    {
        $role = null;
        if (isset($row['role_id'], $row['role_name'], $row['role_display_name'])) {
            $role = new Role(
                isset($row['role_ref_id']) ? (int) $row['role_ref_id'] : null,
                (string) $row['role_name'],
                (string) $row['role_display_name'],
                $row['role_created_at'] ?? null
            );
        }

        return new self(
            isset($row['id']) ? (int) $row['id'] : null,
            isset($row['role_id']) ? (int) $row['role_id'] : 0,
            (string) $row['name'],
            (string) $row['email'],
            $row['password_hash'] ?? null,
            isset($row['active']) ? (bool) $row['active'] : true,
            $row['created_at'] ?? null,
            $row['updated_at'] ?? null,
            $role
        );
    }

    public function publicArray(): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'email' => $this->email,
            'active' => $this->active,
            'created_at' => $this->createdAt,
            'updated_at' => $this->updatedAt,
            'role' => $this->role?->toArray(),
        ];
    }
}

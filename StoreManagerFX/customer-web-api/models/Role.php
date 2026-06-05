<?php

declare(strict_types=1);

final class Role
{
    public function __construct(
        public readonly ?int $id,
        public readonly string $name,
        public readonly string $displayName,
        public readonly ?string $createdAt = null
    ) {
    }

    public static function fromArray(array $row): self
    {
        return new self(
            isset($row['id']) ? (int) $row['id'] : null,
            (string) $row['name'],
            (string) $row['display_name'],
            $row['created_at'] ?? null
        );
    }

    public function toArray(): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'display_name' => $this->displayName,
            'created_at' => $this->createdAt,
        ];
    }
}

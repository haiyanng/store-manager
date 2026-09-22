<?php

declare(strict_types=1);

final class RoleManagementService
{
    private const SYSTEM_ROLES = ['ADMIN', 'STAFF', 'CUSTOMER'];

    public function __construct(
        private readonly RoleRepository $roleRepository
    ) {
    }

    public function list(): array
    {
        return array_map([$this, 'mapRole'], $this->roleRepository->findAll());
    }

    public function get(int $id): array
    {
        $role = $this->roleRepository->findById($id);
        if ($role === null) {
            throw new RuntimeException('Role not found.');
        }

        return $this->mapRole($role);
    }

    public function create(array $payload): array
    {
        $data = $this->validate($payload);

        if ($this->roleRepository->nameExists($data['name'])) {
            throw new DomainException('Role name already exists.');
        }

        $id = $this->roleRepository->create($data);

        return $this->get($id);
    }

    public function update(int $id, array $payload): array
    {
        $existing = $this->roleRepository->findById($id);
        if ($existing === null) {
            throw new RuntimeException('Role not found.');
        }

        $data = $this->validate($payload);

        if ($this->isSystemRole((string) $existing['name']) && $data['name'] !== $existing['name']) {
            throw new DomainException('System role names cannot be changed.');
        }

        if ($this->roleRepository->nameExists($data['name'], $id)) {
            throw new DomainException('Role name already exists.');
        }

        $this->roleRepository->update($id, $data);

        return $this->get($id);
    }

    public function delete(int $id): array
    {
        $existing = $this->roleRepository->findById($id);
        if ($existing === null) {
            throw new RuntimeException('Role not found.');
        }

        if ($this->isSystemRole((string) $existing['name'])) {
            throw new DomainException('System roles cannot be deleted.');
        }

        if ($this->roleRepository->userCount($id) > 0) {
            throw new DomainException('Role is assigned to users and cannot be deleted.');
        }

        $this->roleRepository->delete($id);

        return ['message' => 'Role deleted successfully.'];
    }

    private function validate(array $payload): array
    {
        $name = strtoupper(trim((string) ($payload['name'] ?? '')));
        $displayName = trim((string) ($payload['display_name'] ?? ''));
        $errors = [];

        if ($name === '' || strlen($name) > 50 || !preg_match('/^[A-Z][A-Z0-9_]*$/', $name)) {
            $errors['name'] = 'Role name is required, must be 50 characters or less, and may contain uppercase letters, numbers, and underscores.';
        }

        if ($displayName === '' || strlen($displayName) > 100) {
            $errors['display_name'] = 'Display name is required and must be 100 characters or less.';
        }

        if ($errors !== []) {
            throw new InvalidArgumentException(json_encode($errors, JSON_THROW_ON_ERROR));
        }

        return [
            'name' => $name,
            'display_name' => $displayName,
        ];
    }

    private function isSystemRole(string $name): bool
    {
        return in_array($name, self::SYSTEM_ROLES, true);
    }

    private function mapRole(array $row): array
    {
        return [
            'id' => (int) $row['id'],
            'name' => (string) $row['name'],
            'display_name' => (string) $row['display_name'],
            'created_at' => $row['created_at'] ?? null,
        ];
    }
}

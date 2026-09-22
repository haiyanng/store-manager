<?php

declare(strict_types=1);

final class UserManagementService
{
    public function __construct(
        private readonly UserRepository $userRepository,
        private readonly RoleRepository $roleRepository
    ) {
    }

    public function list(): array
    {
        return array_map([$this, 'mapUser'], $this->userRepository->findAllWithRole());
    }

    public function get(int $id): array
    {
        $user = $this->userRepository->findByIdWithRole($id);
        if ($user === null) {
            throw new RuntimeException('User not found.');
        }

        return $this->mapUser($user);
    }

    public function create(array $payload): array
    {
        $data = $this->validate($payload, true);

        if ($this->roleRepository->findById($data['role_id']) === null) {
            throw new InvalidArgumentException(json_encode(['role_id' => 'Selected role does not exist.'], JSON_THROW_ON_ERROR));
        }

        if ($this->userRepository->emailExists($data['email'])) {
            throw new DomainException('Email already exists.');
        }

        $id = $this->userRepository->create([
            ...$data,
            'password_hash' => password_hash($data['password'], PASSWORD_DEFAULT),
        ]);

        return $this->get($id);
    }

    public function update(int $id, array $payload, int $currentUserId): array
    {
        $existing = $this->userRepository->findByIdWithRole($id);
        if ($existing === null) {
            throw new RuntimeException('User not found.');
        }

        $data = $this->validate($payload, false);

        if ($this->roleRepository->findById($data['role_id']) === null) {
            throw new InvalidArgumentException(json_encode(['role_id' => 'Selected role does not exist.'], JSON_THROW_ON_ERROR));
        }

        if ($this->userRepository->emailExistsExcept($data['email'], $id)) {
            throw new DomainException('Email already exists.');
        }

        if ($id === $currentUserId && !$data['active']) {
            throw new DomainException('You cannot deactivate your own account.');
        }

        $password = (string) ($payload['password'] ?? '');
        $update = [
            'role_id' => $data['role_id'],
            'name' => $data['name'],
            'email' => $data['email'],
            'active' => $data['active'],
        ];

        if ($password !== '') {
            if (strlen($password) < 8) {
                throw new InvalidArgumentException(json_encode(['password' => 'Password must be at least 8 characters.'], JSON_THROW_ON_ERROR));
            }
            $update['password_hash'] = password_hash($password, PASSWORD_DEFAULT);
        }

        $this->userRepository->update($id, $update);

        return $this->get($id);
    }

    public function delete(int $id, int $currentUserId): array
    {
        $existing = $this->userRepository->findByIdWithRole($id);
        if ($existing === null) {
            throw new RuntimeException('User not found.');
        }

        if ($id === $currentUserId) {
            throw new DomainException('You cannot delete your own account.');
        }

        if (($existing['role_name'] ?? '') === 'ADMIN' && $this->userRepository->activeUserCountForRoleName('ADMIN') <= 1) {
            throw new DomainException('The last active administrator cannot be deleted.');
        }

        $this->userRepository->delete($id);

        return ['message' => 'User deleted successfully.'];
    }

    private function validate(array $payload, bool $passwordRequired): array
    {
        $roleId = filter_var($payload['role_id'] ?? null, FILTER_VALIDATE_INT);
        $name = trim((string) ($payload['name'] ?? ''));
        $email = strtolower(trim((string) ($payload['email'] ?? '')));
        $password = (string) ($payload['password'] ?? '');
        $active = array_key_exists('active', $payload) ? filter_var($payload['active'], FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE) : true;
        $errors = [];

        if ($roleId === false || $roleId <= 0) {
            $errors['role_id'] = 'A valid role is required.';
        }

        if ($name === '' || strlen($name) > 150) {
            $errors['name'] = 'Name is required and must be 150 characters or less.';
        }

        if (!filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($email) > 190) {
            $errors['email'] = 'A valid email address is required.';
        }

        if ($passwordRequired && strlen($password) < 8) {
            $errors['password'] = 'Password must be at least 8 characters.';
        }

        if ($active === null) {
            $errors['active'] = 'Active must be true or false.';
        }

        if ($errors !== []) {
            throw new InvalidArgumentException(json_encode($errors, JSON_THROW_ON_ERROR));
        }

        return [
            'role_id' => (int) $roleId,
            'name' => $name,
            'email' => $email,
            'password' => $password,
            'active' => (bool) $active,
        ];
    }

    private function mapUser(array $row): array
    {
        return [
            'id' => (int) $row['id'],
            'role_id' => (int) $row['role_id'],
            'name' => (string) $row['name'],
            'email' => (string) $row['email'],
            'active' => (bool) $row['active'],
            'created_at' => $row['created_at'] ?? null,
            'updated_at' => $row['updated_at'] ?? null,
            'role' => [
                'id' => isset($row['role_ref_id']) ? (int) $row['role_ref_id'] : (int) $row['role_id'],
                'name' => (string) ($row['role_name'] ?? ''),
                'display_name' => (string) ($row['role_display_name'] ?? ''),
            ],
        ];
    }
}

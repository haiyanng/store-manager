<?php

declare(strict_types=1);

final class AuthService
{
    public function __construct(
        private readonly UserRepository $userRepository,
        private readonly RoleRepository $roleRepository
    ) {
    }

    public function register(array $payload): array
    {
        $name = trim((string) ($payload['name'] ?? ''));
        $email = strtolower(trim((string) ($payload['email'] ?? '')));
        $password = (string) ($payload['password'] ?? '');

        $errors = [];

        if ($name === '' || strlen($name) > 150) {
            $errors['name'] = 'Name is required and must be 150 characters or less.';
        }

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            $errors['email'] = 'A valid email address is required.';
        }

        if (strlen($password) < 8) {
            $errors['password'] = 'Password must be at least 8 characters.';
        }

        if ($errors !== []) {
            throw new InvalidArgumentException(json_encode($errors, JSON_THROW_ON_ERROR));
        }

        if ($this->userRepository->emailExists($email)) {
            throw new DomainException('Email already exists.');
        }

        $role = $this->requireRoleByName('CUSTOMER');
        $passwordHash = password_hash($password, PASSWORD_DEFAULT);
        $userId = $this->userRepository->create([
            'role_id' => $role['id'],
            'name' => $name,
            'email' => $email,
            'password_hash' => $passwordHash,
            'active' => 1,
        ]);

        $user = $this->userRepository->findByIdWithRole($userId);
        if ($user === null) {
            throw new RuntimeException('User registration failed.');
        }

        return [
            'user' => $this->mapUser($user),
            'role' => [
                'id' => (int) $role['id'],
                'name' => (string) $role['name'],
                'display_name' => (string) $role['display_name'],
            ],
        ];
    }

    public function login(array $payload): array
    {
        $email = strtolower(trim((string) ($payload['email'] ?? '')));
        $password = (string) ($payload['password'] ?? '');

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            throw new InvalidArgumentException('A valid email address is required.');
        }

        if ($password === '') {
            throw new InvalidArgumentException('Password is required.');
        }

        $user = $this->userRepository->findByEmailWithRole($email);
        if ($user === null) {
            throw new RuntimeException('Invalid email or password.');
        }

        if (!(bool) $user['active']) {
            throw new RuntimeException('User account is inactive.');
        }

        if (!password_verify($password, (string) $user['password_hash'])) {
            throw new RuntimeException('Invalid email or password.');
        }

        $token = bin2hex(random_bytes(32));
        $tokenHash = hash('sha256', $token);
        $expiresAt = (new DateTimeImmutable('+7 days'))->format('Y-m-d H:i:s');
        $this->userRepository->createAuthToken((int) $user['id'], $tokenHash, $expiresAt);

        return [
            'token' => $token,
            'expires_at' => $expiresAt,
            'user' => $this->mapUser($user),
            'role' => $this->mapRole($user),
        ];
    }

    public function authenticateToken(string $token): array
    {
        $token = trim($token);
        if ($token === '') {
            throw new RuntimeException('Authorization token is required.');
        }

        $tokenHash = hash('sha256', $token);
        $row = $this->userRepository->findActiveUserByTokenHash($tokenHash);
        if ($row === null) {
            throw new RuntimeException('Invalid or expired token.');
        }

        if (!empty($row['revoked_at'])) {
            throw new RuntimeException('Token has been revoked.');
        }

        $expiresAt = new DateTimeImmutable((string) $row['expires_at']);
        if ($expiresAt < new DateTimeImmutable()) {
            throw new RuntimeException('Token has expired.');
        }

        if (!(bool) $row['active']) {
            throw new RuntimeException('User account is inactive.');
        }

        return [
            'user' => $this->mapUser($row),
            'role' => $this->mapRole($row),
            'token' => [
                'expires_at' => $row['expires_at'],
            ],
        ];
    }

    public function me(string $token): array
    {
        return $this->authenticateToken($token);
    }

    public function logout(string $token): array
    {
        $token = trim($token);
        if ($token === '') {
            throw new RuntimeException('Authorization token is required.');
        }

        $tokenHash = hash('sha256', $token);
        $this->userRepository->revokeToken($tokenHash);

        return [
            'message' => 'Logged out successfully.',
        ];
    }

    private function requireRoleByName(string $name): array
    {
        $role = $this->roleRepository->findByName($name);
        if ($role === null) {
            throw new RuntimeException(sprintf('Required role "%s" does not exist.', $name));
        }

        return $role;
    }

    private function mapUser(array $row): array
    {
        return [
            'id' => (int) $row['id'],
            'name' => (string) $row['name'],
            'email' => (string) $row['email'],
            'active' => (bool) $row['active'],
            'created_at' => $row['created_at'] ?? null,
            'updated_at' => $row['updated_at'] ?? null,
        ];
    }

    private function mapRole(array $row): array
    {
        return [
            'id' => isset($row['role_id']) ? (int) $row['role_id'] : (int) $row['role_ref_id'],
            'name' => (string) ($row['role_name'] ?? ''),
            'display_name' => (string) ($row['role_display_name'] ?? ''),
        ];
    }
}

<?php

declare(strict_types=1);

final class AuthGuard
{
    public function __construct(private readonly AuthService $authService)
    {
    }

    public function requireAuth(Request $request): array
    {
        $token = $request->bearerToken();
        if ($token === null || $token === '') {
            throw new RuntimeException('Authorization token is required.');
        }

        return $this->authService->authenticateToken($token);
    }

    public function requireRole(Request $request, array $allowedRoles): array
    {
        $context = $this->requireAuth($request);
        $roleName = $context['role']['name'] ?? null;

        if (!is_string($roleName) || !in_array($roleName, $allowedRoles, true)) {
            throw new RuntimeException('Permission denied.');
        }

        return $context;
    }

    public function hasRole(array $context, string|array $allowedRoles): bool
    {
        $roles = is_array($allowedRoles) ? $allowedRoles : [$allowedRoles];
        $roleName = $context['role']['name'] ?? null;

        return is_string($roleName) && in_array($roleName, $roles, true);
    }
}

<?php

declare(strict_types=1);

final class AuthController
{
    public function __construct(private readonly AuthService $authService)
    {
    }

    public function register(Request $request): void
    {
        try {
            $result = $this->authService->register($request->all());
            Response::success($result, 201);
        } catch (InvalidArgumentException $exception) {
            Response::error('Validation failed.', 422, $this->decodeErrors($exception->getMessage()));
        } catch (DomainException $exception) {
            Response::error($exception->getMessage(), 409);
        } catch (Throwable $exception) {
            $this->logException($exception);
            Response::error('Registration failed.', 500);
        }
    }

    public function login(Request $request): void
    {
        try {
            $result = $this->authService->login($request->all());
            Response::success($result, 200);
        } catch (InvalidArgumentException $exception) {
            Response::error($exception->getMessage(), 422);
        } catch (RuntimeException $exception) {
            Response::error($exception->getMessage(), 401);
        } catch (Throwable $exception) {
            $this->logException($exception);
            Response::error('Login failed.', 500);
        }
    }

    public function me(Request $request): void
    {
        try {
            $token = $this->requireToken($request);
            $result = $this->authService->me($token);
            Response::success($result, 200);
        } catch (RuntimeException $exception) {
            Response::error($exception->getMessage(), 401);
        } catch (Throwable $exception) {
            $this->logException($exception);
            Response::error('Failed to load current user.', 500);
        }
    }

    public function logout(Request $request): void
    {
        try {
            $token = $this->requireToken($request);
            $result = $this->authService->logout($token);
            Response::success($result, 200);
        } catch (RuntimeException $exception) {
            Response::error($exception->getMessage(), 401);
        } catch (Throwable $exception) {
            $this->logException($exception);
            Response::error('Logout failed.', 500);
        }
    }

    private function requireToken(Request $request): string
    {
        $token = $request->bearerToken();
        if ($token === null || $token === '') {
            throw new RuntimeException('Authorization token is required.');
        }

        return $token;
    }

    private function decodeErrors(string $message): array
    {
        $decoded = json_decode($message, true);
        return is_array($decoded) ? $decoded : [];
    }

    private function logException(Throwable $exception): void
    {
        $logDir = __DIR__ . '/../storage/logs';
        if (!is_dir($logDir)) {
            @mkdir($logDir, 0775, true);
        }

        $entry = sprintf(
            "[%s] %s: %s in %s:%d\n",
            (new DateTimeImmutable())->format(DATE_ATOM),
            $exception::class,
            $exception->getMessage(),
            $exception->getFile(),
            $exception->getLine()
        );
        @file_put_contents($logDir . '/app.log', $entry, FILE_APPEND);
    }
}

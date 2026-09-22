<?php

declare(strict_types=1);

final class UserController
{
    public function __construct(
        private readonly UserManagementService $userService,
        private readonly AuthGuard $authGuard
    ) {
    }

    public function index(Request $request): void
    {
        $this->runAuthenticated($request, fn () => Response::success($this->userService->list()));
    }

    public function show(Request $request, int $id): void
    {
        $this->runAuthenticated($request, fn () => Response::success($this->userService->get($id)));
    }

    public function store(Request $request): void
    {
        $this->runAuthenticated($request, fn () => Response::success($this->userService->create($request->all()), 201));
    }

    public function update(Request $request, int $id): void
    {
        $this->runAuthenticated($request, fn (array $context) => Response::success(
            $this->userService->update($id, $request->all(), (int) $context['user']['id'])
        ));
    }

    public function destroy(Request $request, int $id): void
    {
        $this->runAuthenticated($request, fn (array $context) => Response::success(
            $this->userService->delete($id, (int) $context['user']['id'])
        ));
    }

    private function runAuthenticated(Request $request, callable $action): void
    {
        try {
            $context = $this->authGuard->requireRole($request, ['ADMIN']);
            $action($context);
        } catch (InvalidArgumentException $exception) {
            Response::error('Validation failed.', 422, $this->decodeErrors($exception->getMessage()));
        } catch (DomainException $exception) {
            Response::error($exception->getMessage(), 409);
        } catch (RuntimeException $exception) {
            $message = $exception->getMessage();
            Response::error($message, str_contains($message, 'not found') ? 404 : 401);
        }
    }

    private function decodeErrors(string $message): array
    {
        $decoded = json_decode($message, true);
        return is_array($decoded) ? $decoded : [];
    }
}

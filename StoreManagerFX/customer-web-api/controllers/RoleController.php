<?php

declare(strict_types=1);

final class RoleController
{
    public function __construct(
        private readonly RoleManagementService $roleService,
        private readonly AuthGuard $authGuard
    ) {
    }

    public function index(Request $request): void
    {
        $this->runAuthenticated($request, fn () => Response::success($this->roleService->list()));
    }

    public function show(Request $request, int $id): void
    {
        $this->runAuthenticated($request, fn () => Response::success($this->roleService->get($id)));
    }

    public function store(Request $request): void
    {
        $this->runAuthenticated($request, fn () => Response::success($this->roleService->create($request->all()), 201));
    }

    public function update(Request $request, int $id): void
    {
        $this->runAuthenticated($request, fn () => Response::success($this->roleService->update($id, $request->all())));
    }

    public function destroy(Request $request, int $id): void
    {
        $this->runAuthenticated($request, fn () => Response::success($this->roleService->delete($id)));
    }

    private function runAuthenticated(Request $request, callable $action): void
    {
        try {
            $this->authGuard->requireRole($request, ['ADMIN']);
            $action();
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

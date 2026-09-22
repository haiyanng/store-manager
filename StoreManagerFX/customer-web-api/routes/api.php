<?php

declare(strict_types=1);

return static function (
    Request $request,
    AuthController $authController,
    RoleController $roleController,
    UserController $userController
): void {
    $method = $request->method();
    $path = rtrim($request->path(), '/');
    $path = $path === '' ? '/' : $path;

    if ($method === 'POST' && $path === '/api/auth/register') {
        $authController->register($request);
        return;
    }

    if ($method === 'POST' && $path === '/api/auth/login') {
        $authController->login($request);
        return;
    }

    if ($method === 'GET' && $path === '/api/auth/me') {
        $authController->me($request);
        return;
    }

    if ($method === 'POST' && $path === '/api/auth/logout') {
        $authController->logout($request);
        return;
    }

    if ($method === 'GET' && $path === '/api/roles') {
        $roleController->index($request);
        return;
    }

    if ($method === 'POST' && $path === '/api/roles') {
        $roleController->store($request);
        return;
    }

    if (preg_match('#^/api/roles/(\d+)$#', $path, $matches) === 1) {
        $id = (int) $matches[1];

        if ($method === 'GET') {
            $roleController->show($request, $id);
            return;
        }

        if ($method === 'PUT') {
            $roleController->update($request, $id);
            return;
        }

        if ($method === 'DELETE') {
            $roleController->destroy($request, $id);
            return;
        }
    }

    if ($method === 'GET' && $path === '/api/users') {
        $userController->index($request);
        return;
    }

    if ($method === 'POST' && $path === '/api/users') {
        $userController->store($request);
        return;
    }

    if (preg_match('#^/api/users/(\d+)$#', $path, $matches) === 1) {
        $id = (int) $matches[1];

        if ($method === 'GET') {
            $userController->show($request, $id);
            return;
        }

        if ($method === 'PUT') {
            $userController->update($request, $id);
            return;
        }

        if ($method === 'DELETE') {
            $userController->destroy($request, $id);
            return;
        }
    }

    Response::error('Not Found', 404);
};

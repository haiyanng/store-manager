<?php

declare(strict_types=1);

return static function (Request $request, AuthController $authController): void {
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

    Response::error('Not Found', 404);
};

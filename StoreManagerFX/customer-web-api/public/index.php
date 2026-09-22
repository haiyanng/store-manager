<?php

declare(strict_types=1);

require_once __DIR__ . '/../core/Database.php';
require_once __DIR__ . '/../core/Request.php';
require_once __DIR__ . '/../core/Response.php';
require_once __DIR__ . '/../core/AuthGuard.php';
require_once __DIR__ . '/../models/Role.php';
require_once __DIR__ . '/../models/User.php';
require_once __DIR__ . '/../repositories/RoleRepository.php';
require_once __DIR__ . '/../repositories/UserRepository.php';
require_once __DIR__ . '/../services/AuthService.php';
require_once __DIR__ . '/../services/RoleManagementService.php';
require_once __DIR__ . '/../services/UserManagementService.php';
require_once __DIR__ . '/../controllers/AuthController.php';
require_once __DIR__ . '/../controllers/RoleController.php';
require_once __DIR__ . '/../controllers/UserController.php';

spl_autoload_register(static function (string $class): void {
    $baseDir = dirname(__DIR__);
    $folders = ['core', 'models', 'repositories', 'services', 'controllers'];

    foreach ($folders as $folder) {
        $file = $baseDir . DIRECTORY_SEPARATOR . $folder . DIRECTORY_SEPARATOR . $class . '.php';
        if (is_file($file)) {
            require_once $file;
            return;
        }
    }
});

function customer_web_api_log(Throwable $exception): void
{
    $logDir = dirname(__DIR__) . '/storage/logs';
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

$allowedOrigins = [
    'http://localhost:3000',
    'http://localhost:5173',
];

$origin = $_SERVER['HTTP_ORIGIN'] ?? '';
if (is_string($origin) && in_array($origin, $allowedOrigins, true)) {
    header('Access-Control-Allow-Origin: ' . $origin);
    header('Vary: Origin');
}

header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
header('Access-Control-Allow-Credentials: true');
header('Content-Type: application/json; charset=utf-8');

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

try {
    $pdo = Database::connection();
    $roleRepository = new RoleRepository($pdo);
    $userRepository = new UserRepository($pdo);
    $authService = new AuthService($userRepository, $roleRepository);
    $authGuard = new AuthGuard($authService);
    $authController = new AuthController($authService);
    $roleController = new RoleController(new RoleManagementService($roleRepository), $authGuard);
    $userController = new UserController(new UserManagementService($userRepository, $roleRepository), $authGuard);

    $request = Request::fromGlobals();
    $dispatch = require __DIR__ . '/../routes/api.php';
    $dispatch($request, $authController, $roleController, $userController);
} catch (Throwable $exception) {
    customer_web_api_log($exception);
    Response::error('Internal server error.', 500);
}

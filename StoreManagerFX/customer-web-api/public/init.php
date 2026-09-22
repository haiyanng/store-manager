<?php

declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');

require_once __DIR__ . '/../core/Database.php';

function customer_web_api_init_log(Throwable $exception): void
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
    @file_put_contents($logDir . '/init.log', $entry, FILE_APPEND);
}

function split_sql_statements(string $sql): array
{
    $statements = array_map(
        static fn (string $statement): string => trim($statement),
        explode(';', $sql)
    );

    return array_values(array_filter(
        $statements,
        static fn (string $statement): bool => $statement !== ''
    ));
}

try {
    $config = require __DIR__ . '/../config/db_config.php';
    $adminPasswordHash = password_hash('admin123456', PASSWORD_DEFAULT);
    $sql = file_get_contents(__DIR__ . '/../database/init.sql');

    if ($sql === false || trim($sql) === '') {
        throw new RuntimeException('init.sql could not be read.');
    }

    $sql = str_replace('{{ADMIN_PASSWORD_HASH}}', $adminPasswordHash, $sql);

    $serverConnection = Database::serverConnection();
    foreach (split_sql_statements($sql) as $statement) {
        $serverConnection->exec($statement);
    }

    $dbConnection = Database::connection();
    $counts = [];
    foreach (['roles', 'users', 'auth_tokens'] as $table) {
        $counts[$table] = (int) $dbConnection->query("SELECT COUNT(*) FROM {$table}")->fetchColumn();
    }

    echo json_encode([
        'success' => true,
        'data' => [
            'message' => 'Database initialized successfully.',
            'database' => $config['name'],
            'counts' => $counts,
            'default_admin' => [
                'email' => 'admin@customer.local',
                'password' => 'admin123456',
            ],
        ],
    ], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR);
} catch (Throwable $exception) {
    customer_web_api_init_log($exception);
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => $exception->getMessage(),
        'errors' => (object) [],
    ], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR);
}

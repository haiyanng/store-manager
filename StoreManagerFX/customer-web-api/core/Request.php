<?php

declare(strict_types=1);

final class Request
{
    private string $method;
    private string $path;
    private array $headers;
    private array $query;
    private array $body;
    private string $rawBody;

    private function __construct(
        string $method,
        string $path,
        array $headers,
        array $query,
        array $body,
        string $rawBody
    ) {
        $this->method = $method;
        $this->path = $path;
        $this->headers = $headers;
        $this->query = $query;
        $this->body = $body;
        $this->rawBody = $rawBody;
    }

    public static function fromGlobals(): self
    {
        $rawBody = file_get_contents('php://input') ?: '';
        $headers = function_exists('getallheaders') ? getallheaders() : [];
        $normalizedHeaders = [];

        foreach ($headers as $name => $value) {
            $normalizedHeaders[strtolower((string) $name)] = (string) $value;
        }

        $body = [];
        if ($rawBody !== '') {
            $decoded = json_decode($rawBody, true);
            if (is_array($decoded)) {
                $body = $decoded;
            }
        }

        if ($body === [] && $_POST !== []) {
            $body = $_POST;
        }

        $uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';

        return new self(
            strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET'),
            $uri,
            $normalizedHeaders,
            $_GET,
            $body,
            $rawBody
        );
    }

    public function method(): string
    {
        return $this->method;
    }

    public function path(): string
    {
        return $this->path;
    }

    public function query(string $key, mixed $default = null): mixed
    {
        return $this->query[$key] ?? $default;
    }

    public function input(string $key, mixed $default = null): mixed
    {
        return $this->body[$key] ?? $default;
    }

    public function all(): array
    {
        return $this->body;
    }

    public function header(string $name, ?string $default = null): ?string
    {
        $key = strtolower($name);
        return $this->headers[$key] ?? $default;
    }

    public function bearerToken(): ?string
    {
        $authorization = $this->header('authorization');
        if (!is_string($authorization) || $authorization === '') {
            return null;
        }

        if (!preg_match('/^Bearer\s+(.+)$/i', trim($authorization), $matches)) {
            return null;
        }

        return trim($matches[1]);
    }

    public function rawBody(): string
    {
        return $this->rawBody;
    }
}

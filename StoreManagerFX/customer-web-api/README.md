# customer-web-api

Plain PHP + MySQL backend for `customer-web`.

## Requirements

- PHP 8.2+
- MySQL running locally

## Setup

1. Edit `config/db_config.php` if your local MySQL credentials differ.
2. Initialize the database:

```bash
php public/init.php
```

3. Start the API server:

```bash
php -S localhost:8000 -t public
```

## Auth Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

## Sample curl

### Register

```bash
curl -X POST http://localhost:8000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer A",
    "email": "customer@example.com",
    "password": "password123"
  }'
```

### Login

```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@customer.local",
    "password": "admin123456"
  }'
```

### Me

```bash
curl http://localhost:8000/api/auth/me \
  -H "Authorization: Bearer <token>"
```

### Logout

```bash
curl -X POST http://localhost:8000/api/auth/logout \
  -H "Authorization: Bearer <token>"
```

## Default Admin

- Email: `admin@customer.local`
- Password: `admin123456`

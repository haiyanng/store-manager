# StoreManagerFX Customer API and Desktop Shop

This workspace keeps the existing `StoreManagerFX` app untouched and adds two independent sibling projects:

- `store-api`: Spring Boot REST API for customer shopping.
- `CustomerShopFX`: JavaFX desktop customer shop that talks only to the REST API.

## MySQL setup

1. Start MySQL locally.
2. Create the same database used by the existing app:

```sql
CREATE DATABASE IF NOT EXISTS family_business_manager_db;
```

3. Update `store-api/src/main/resources/application.properties` or copy `application-example.properties` and adjust:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/family_business_manager_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```

The API uses additive SQL scripts. It reuses existing `categories` and `products` tables and creates customer-facing tables such as `customers`, `carts`, `cart_items`, `orders`, and `order_items`.

Sample login seeded by `data.sql`:

- Email: `customer@example.com`
- Password: `password`

## Run the old StoreManagerFX app

From the existing project folder:

```powershell
cd StoreManagerFX\StoreManagerFX
mvn javafx:run
```

## Run the API

```powershell
cd store-api
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

## Run CustomerShopFX

```powershell
cd CustomerShopFX
mvn javafx:run
```

To point the desktop app at a different API URL:

```powershell
mvn javafx:run -Dstore.api.url=http://localhost:8080
```

## REST endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/categories`
- `GET /api/cart`
- `POST /api/cart/items`
- `PUT /api/cart/items/{id}`
- `DELETE /api/cart/items/{id}`
- `DELETE /api/cart/clear`
- `POST /api/orders`
- `GET /api/orders/my`
- `GET /api/orders/{id}`
- `PUT /api/orders/{id}/cancel`
- `GET /api/customers/profile`
- `PUT /api/customers/profile`
- `PUT /api/customers/password`

# StoreManagerFX Agent Instructions

StoreManagerFX is a Java 21 + JavaFX + MySQL JDBC desktop application.

## Architecture Rules

### 1. Layer Flow

Follow this flow:

```text
FXML
-> Controller
-> Presenter
-> Service
-> Repository
-> Database
```

### 2. Controller Rules

- Controllers only read/write UI fields.
- Controllers call Presenters.
- Controllers must not call Repository directly.
- Controllers must not query database.
- Controllers must not contain business rules.

### 3. Presenter Rules

- Presenters orchestrate screen workflow.
- Presenters call Services.
- Presenters may coordinate validation, selection state, refresh flow, and navigation inside module content.
- Presenters must not query database directly.

### 4. Service Rules

- Services contain business logic.
- Services validate runtime permissions when needed.
- Services call Repositories.
- Services must not use JavaFX UI classes.

### 5. Repository Rules

- Repositories are the only layer allowed to use JDBC SQL queries.
- Repositories must use ConnectionFactory.
- Repositories must not call JavaFX or SceneManager.

### 6. Core Rules

- `core/database`, `core/navigation`, `core/session`, `core/security` are owned by lead core.
- Do not modify core classes unless explicitly asked.
- Do not create duplicate `SceneManager`, `ContentManager`, `AppSession`, `AuthService`, or `ConnectionFactory`.

### 7. Domain Rules

- User and Employee are separate domains.
- Employee may reference User by `userId` only.
- Do not put a `User` object inside `Employee`.
- Do not create ORM-like object graphs.

### 8. Role Rules

- Use `RoleType` enum.
- Do not hard-code role strings.
- `DEVELOPER` is the unique technical root account.
- UI must not create `DEVELOPER`.
- `OWNER` cannot edit/delete `DEVELOPER`.
- `MANAGER` and `EMPLOYEE` cannot access user management.

### 9. Resource Rules

- FXML files go in `src/main/resources/fxml`.
- CSS files go in `src/main/resources/css`.
- Java classes go in `src/main/java`.
- Do not create FXML inside `src/main/java`.

### 10. Testing

After changes, run:

```bash
mvn clean javafx:run
```

### 11. Refactor Safety

- Do not rewrite the whole project.
- Do not change package structure without explicit instruction.
- Make small focused changes.
- Explain what files were changed and why.

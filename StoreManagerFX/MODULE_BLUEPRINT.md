# Module Blueprint

This blueprint standardizes future StoreManagerFX modules for a Java 21 + JavaFX + MySQL JDBC desktop runtime.

Reference implementation: `domain/employee`.

## 1. Standard Folder Structure

Each domain module should use:

```text
src/main/java/com/storemanager/domain/<module>/
├── model
├── repository
├── service
├── presenter
└── view

src/main/resources/fxml/<module>/
├── <module>-list.fxml
└── <module>-form.fxml
```

Use additional FXML files only when the screen is naturally split into reusable views.

## 2. Naming Conventions

For module `employee`:

- Model: `Employee`
- Repository: `EmployeeRepository`
- Service: `EmployeeService`
- Presenter: `EmployeePresenter`
- List controller: `EmployeeListController`
- Form controller: `EmployeeFormController`
- List FXML: `employee-list.fxml`
- Form FXML: `employee-form.fxml`

Use singular nouns for Java classes and lowercase kebab-case for FXML files.

## 3. Layer Responsibilities

Required flow:

```text
FXML
-> Controller
-> Presenter
-> Service
-> Repository
-> Database
```

No layer should skip downward unless explicitly approved.

## 4. Runtime Flow

1. `ContentManager` loads the module list FXML into the shell center pane.
2. JavaFX creates the controller from FXML.
3. Controller initializes UI bindings and creates the presenter.
4. Presenter loads data through service.
5. Service calls repository.
6. Repository runs JDBC through `ConnectionFactory`.
7. Presenter updates the controller view API.
8. Controller writes data to JavaFX controls.

## 5. MVP Orchestration Rules

Controllers are view adapters. Presenters orchestrate screen behavior.

Controllers may:

- Bind `TableColumn` cell value factories.
- Read form fields.
- Write form/table/status UI.
- Show alerts.
- Forward UI events to presenter.

Controllers must not:

- Call repositories.
- Query database.
- Hold business rules.
- Hold permission rules.
- Own selection workflow.

Presenters may:

- Hold selected entity state.
- Coordinate create/update/delete flows.
- Decide CRUD mode transitions.
- Refresh table data.
- Resolve display-only linked data through services.
- Ask controller to update UI state.

## 6. CRUD Runtime State Rules

Use:

- `CrudMode`
- `ModuleState<T>`
- `BaseCrudPresenter<T>`

Standard modes:

- `CREATE`: no selected entity, form creates new record.
- `EDIT`: selected entity exists, form updates selected record.
- `VIEW`: selected entity exists, form is display-only if needed.

Presenter should call:

- `enterCreateMode()` when clearing the form.
- `enterEditMode(entity)` when selecting a row for editing.
- `enterViewMode(entity)` only for read-only screens.

Do not duplicate `selectedEntity` state inside controllers.

## 7. Async Task Rules

Use `AsyncTaskRunner` for long-running work.

Examples:

- Database backup.
- Database restore.
- File import/export.
- Large reports.

Rules:

- Do not create unmanaged threads in controllers.
- Do not run long operations on JavaFX Application Thread.
- Presenter owns async orchestration.
- Controller only updates UI state.
- Use `LoadingState` for `IDLE`, `LOADING`, `SUCCESS`, `ERROR`.

## 8. Permission Integration Rules

Use `PermissionGuard`.

Rules:

- Sidebar visibility uses `PermissionGuard`.
- Runtime service validation must also use `PermissionGuard` when security matters.
- Do not trust UI checks only.
- Use `RoleType`, never hard-coded role strings.
- Do not duplicate permission logic inside controllers.

## 9. ContentManager Integration Rules

Shell sidebar actions should call `ContentManager.loadContent(...)`.

Example:

```java
ContentManager.loadContent("/fxml/employee/employee-list.fxml");
```

Modules should not replace the whole scene unless the flow is truly app-level navigation.

## 10. JavaFX Controller Rules

Controllers should stay thin.

Allowed:

- `@FXML` fields.
- `initialize()` UI binding.
- `onCreate`, `onUpdate`, `onDelete`, `onRefresh`, `onClear` event forwarding.
- View methods such as `setEmployees(...)`, `showEmployee(...)`, `showError(...)`.

Avoid:

- SQL.
- Repository fields.
- Business validation.
- Permission branching.
- Session mutation except in shell/auth flows already established.

## 11. Presenter Rules

Presenters should be plain Java classes.

They should:

- Own workflow.
- Own selected entity and mode through `BaseCrudPresenter`.
- Call services.
- Convert service results into view updates.
- Keep controller simple.

They should not:

- Use JDBC.
- Use FXML annotations.
- Own JavaFX controls directly.

## 12. Service Rules

Services contain business logic.

They should:

- Validate domain data.
- Enforce runtime permissions where needed.
- Call repositories.
- Preserve domain boundaries.

They should not:

- Show alerts.
- Use JavaFX controls.
- Use `SceneManager` or `ContentManager`.

## 13. Repository Rules

Repositories are the only module layer that writes JDBC SQL.

They should:

- Use `ConnectionFactory`.
- Return domain models or primitive success/failure values.
- Keep SQL local and explicit.
- Avoid JavaFX dependencies.

They should not:

- Call presenters/controllers.
- Call `SceneManager`.
- Contain UI messages.

## 14. UI/CSS Conventions

FXML goes under `src/main/resources/fxml/<module>/`.

Shared CSS should go under `src/main/resources/css/` for new work. Existing module-local CSS can be migrated gradually.

Use existing style classes where possible:

- `content-title`
- `content-description`
- `module-input`
- `module-button`

Keep layouts compact, readable, and scalable inside the dashboard shell.

## 15. TableView Conventions

TableView setup belongs in controller `initialize()`.

Use:

- `PropertyValueFactory` for simple model fields.
- `SimpleStringProperty` for derived display values.

Derived values should be provided by presenter or service-backed presenter methods, not direct repository access from controller.

## 16. Form Conventions

Use a separate form FXML/controller when the form is reusable or large.

Form controller may:

- Read field values into a model.
- Populate fields from a model.
- Clear fields.

Form controller must not:

- Save data.
- Query data.
- Call repository.
- Own business validation.

## 17. Validation Flow Conventions

Validation belongs in service.

Flow:

1. Controller reads form into model.
2. Presenter receives model.
3. Presenter calls service.
4. Service validates.
5. Service throws clear runtime errors or returns success/failure.
6. Presenter forwards user-facing error to controller.
7. Controller shows alert.

## 18. Error Handling Conventions

Keep errors explicit and user-readable.

Controller:

- Shows alerts.
- Updates status labels.
- Does not decide business meaning.

Presenter:

- Catches service exceptions.
- Maps operation failure to clear UI messages.
- Resets loading state when async work finishes.

Service:

- Throws clear `RuntimeException` messages for validation and permission failures.

Repository:

- Catches SQL exceptions where appropriate.
- Returns empty list or `false` for operation failure.
- Should not show UI alerts.

## Recommended Module Build Order

1. Model
2. Repository
3. Service
4. Presenter
5. FXML
6. Controller
7. Sidebar `ContentManager` integration
8. `mvn test`

Keep changes focused. Avoid broad rewrites and avoid generic frameworks that obscure simple JavaFX runtime behavior.

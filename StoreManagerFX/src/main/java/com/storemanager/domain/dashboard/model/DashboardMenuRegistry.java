package com.storemanager.domain.dashboard.model;

import com.storemanager.domain.user.model.RoleType;

import java.util.List;
import java.util.Set;

public final class DashboardMenuRegistry {

    public static final String DASHBOARD = "dashboard";
    public static final String EMPLOYEE = "employee";
    public static final String ATTENDANCE = "attendance";
    public static final String ATTENDANCE_ANOMALIES = "attendance_anomalies";
    public static final String USER_MANAGEMENT = "user_management";
    public static final String PRODUCT = "product";
    public static final String CATEGORY = "category";
    public static final String INVENTORY = "inventory";
    public static final String IMPORT = "import";
    public static final String ORDER = "order";
    public static final String ONLINE_ORDER = "online_order";
    public static final String SYSTEM_TOOLS = "system_tools";
    public static final String AUDIT_LOGS = "audit_logs";
    public static final String NOTIFICATIONS = "notifications";
    public static final String MESSAGES = "messages";

    private static final Set<RoleType> ALL_ROLES =
            Set.of(
                    RoleType.OWNER,
                    RoleType.MANAGER,
                    RoleType.STAFF,
                    RoleType.VIEWER,
                    RoleType.EMPLOYEE
            );

    private static final Set<RoleType> MANAGER_WORKFLOW_ROLES =
            Set.of(
                    RoleType.OWNER,
                    RoleType.MANAGER
            );

    private static final Set<RoleType> STAFF_WORKFLOW_ROLES =
            Set.of(
                    RoleType.OWNER,
                    RoleType.MANAGER,
                    RoleType.STAFF,
                    RoleType.EMPLOYEE
            );

    private static final Set<RoleType> PRODUCT_VIEW_ROLES =
            Set.of(
                    RoleType.OWNER,
                    RoleType.MANAGER,
                    RoleType.STAFF,
                    RoleType.VIEWER,
                    RoleType.EMPLOYEE
            );

    private static final Set<RoleType> VIEWER_REPORT_ROLES =
            Set.of(
                    RoleType.OWNER,
                    RoleType.MANAGER,
                    RoleType.VIEWER
            );

    private static final Set<RoleType> ADMIN_ROLES =
            Set.of(
                    RoleType.OWNER
            );

    private static final List<DashboardMenuItem> MENU_ITEMS =
            List.of(
                    new DashboardMenuItem(
                            DASHBOARD,
                            "Dashboard",
                            "/fxml/dashboard/dashboard-home.fxml",
                            true,
                            ALL_ROLES
                    ),
                    new DashboardMenuItem(
                            EMPLOYEE,
                            "Employee",
                            "/fxml/employee/employee-list.fxml",
                            true,
                            MANAGER_WORKFLOW_ROLES
                    ),
                    new DashboardMenuItem(
                            ATTENDANCE,
                            "Attendance",
                            "/fxml/attendance/attendance.fxml",
                            true,
                            STAFF_WORKFLOW_ROLES
                    ),
                    new DashboardMenuItem(
                            ATTENDANCE_ANOMALIES,
                            "Attendance Anomalies",
                            "/fxml/attendance_anomaly/attendance-anomaly-list.fxml",
                            true,
                            MANAGER_WORKFLOW_ROLES
                    ),
                    new DashboardMenuItem(
                            USER_MANAGEMENT,
                            "User Management",
                            "/fxml/user/user-management.fxml",
                            true,
                            ADMIN_ROLES
                    ),
                    new DashboardMenuItem(
                            PRODUCT,
                            "Product",
                            "/fxml/product/product-list.fxml",
                            true,
                            PRODUCT_VIEW_ROLES
                    ),
                    new DashboardMenuItem(
                            CATEGORY,
                            "Category",
                            "/fxml/category/category-list.fxml",
                            true,
                            PRODUCT_VIEW_ROLES
                    ),
                    new DashboardMenuItem(
                            INVENTORY,
                            "View Inventory",
                            "/fxml/inventory/inventory-list.fxml",
                            true,
                            STAFF_WORKFLOW_ROLES
                    ),
                    new DashboardMenuItem(
                            IMPORT,
                            "Import",
                            "/fxml/importing/import.fxml",
                            true,
                            MANAGER_WORKFLOW_ROLES
                    ),
                    new DashboardMenuItem(
                            ORDER,
                            "Order",
                            "/fxml/sale/sale.fxml",
                            true,
                            STAFF_WORKFLOW_ROLES
                    ),
                    new DashboardMenuItem(
                            ONLINE_ORDER,
                            "Online Orders",
                            "/fxml/onlineorder/online-order.fxml",
                            true,
                            MANAGER_WORKFLOW_ROLES
                    ),
                    new DashboardMenuItem(
                            SYSTEM_TOOLS,
                            "System Tools",
                            "/fxml/system_tool/system-tool.fxml",
                            true,
                            ADMIN_ROLES
                    ),
                    new DashboardMenuItem(
                            AUDIT_LOGS,
                            "Audit Logs",
                            "/fxml/audit/audit-log-viewer.fxml",
                            true,
                            ADMIN_ROLES
                    ),
                    new DashboardMenuItem(
                            NOTIFICATIONS,
                            "Notifications",
                            "/fxml/notification/notification-center.fxml",
                            false,
                            STAFF_WORKFLOW_ROLES
                    ),
                    new DashboardMenuItem(
                            MESSAGES,
                            "Messages",
                            "/fxml/message/message-inbox.fxml",
                            false,
                            STAFF_WORKFLOW_ROLES
                    )
            );

    private DashboardMenuRegistry() {
    }

    public static List<DashboardMenuItem> all() {

        return MENU_ITEMS;
    }
}

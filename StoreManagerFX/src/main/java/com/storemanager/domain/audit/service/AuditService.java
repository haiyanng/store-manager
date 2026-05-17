package com.storemanager.domain.audit.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.model.AuditLog;
import com.storemanager.domain.audit.model.AuditLogFilter;
import com.storemanager.domain.audit.model.AuditLogViewDto;
import com.storemanager.domain.audit.repository.AuditLogRepository;
import com.storemanager.domain.user.model.User;

import java.util.List;

public class AuditService {

    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String ACTION_PAYROLL_GENERATED = "PAYROLL_GENERATED";
    public static final String ACTION_SALARY_CONFIG_UPDATED = "SALARY_CONFIG_UPDATED";
    public static final String ACTION_EMPLOYEE_BRANCH_ASSIGNMENT = "EMPLOYEE_BRANCH_ASSIGNMENT";
    public static final String ACTION_ATTENDANCE_SELF_CHECK_IN = "ATTENDANCE_SELF_CHECK_IN";
    public static final String ACTION_ATTENDANCE_SELF_CHECK_OUT = "ATTENDANCE_SELF_CHECK_OUT";
    public static final String ACTION_INVENTORY_ADJUSTMENT = "INVENTORY_ADJUSTMENT";
    public static final String ACTION_IMPORT_FINALIZED = "IMPORT_FINALIZED";
    public static final String ACTION_SALE_FINALIZED = "SALE_FINALIZED";
    public static final String ACTION_BACKUP = "BACKUP";
    public static final String ACTION_RESTORE = "RESTORE";
    public static final String ACTION_DATABASE_RESET = "DATABASE_RESET";

    private final AuditLogRepository auditLogRepository =
            new AuditLogRepository();

    public boolean record(
            String action,
            String entityType,
            Long entityId,
            String details,
            Long branchId
    ) {

        if (action == null || action.trim().isEmpty()) {
            throw new RuntimeException("Audit action is required");
        }

        if (entityType == null || entityType.trim().isEmpty()) {
            throw new RuntimeException("Audit entity type is required");
        }

        AuditLog log =
                new AuditLog();

        User currentUser =
                AppSession.getCurrentUser();

        log.setUserId(
                currentUser == null ? null : currentUser.getId()
        );
        log.setAction(action.trim());
        log.setEntityType(entityType.trim());
        log.setEntityId(entityId);
        log.setDetails(clean(details));
        log.setBranchId(branchId);

        return auditLogRepository.save(log);
    }

    public void recordPermissionDenied(
            String action,
            String entityType,
            Long entityId,
            String details,
            Long branchId
    ) {

        record(
                ACTION_PERMISSION_DENIED,
                entityType == null ? "SECURITY" : entityType,
                entityId,
                action == null
                        ? details
                        : action + (details == null ? "" : " | " + details),
                branchId
        );
    }

    public void recordLogin(
            User user
    ) {

        if (user == null) {
            return;
        }

        AuditLog log =
                new AuditLog();
        log.setUserId(user.getId());
        log.setAction(ACTION_LOGIN);
        log.setEntityType("USER");
        log.setEntityId(user.getId());
        log.setDetails("User logged in");

        auditLogRepository.save(log);
    }

    public void recordLogout(
            User user
    ) {

        if (user == null) {
            return;
        }

        AuditLog log =
                new AuditLog();
        log.setUserId(user.getId());
        log.setAction(ACTION_LOGOUT);
        log.setEntityType("USER");
        log.setEntityId(user.getId());
        log.setDetails("User logged out");

        auditLogRepository.save(log);
    }

    public List<AuditLogViewDto> findAuditLogs(
            AuditLogFilter filter
    ) {

        validateAccess();

        return auditLogRepository.findAll(filter);
    }

    public void recordDatabaseReset(
            String details
    ) {

        record(
                ACTION_DATABASE_RESET,
                "DATABASE",
                null,
                details,
                null
        );
    }

    private void validateAccess() {

        if (!PermissionGuard.canViewAuditLogs()) {
            recordPermissionDenied(
                    ACTION_PERMISSION_DENIED,
                    "AUDIT_LOG",
                    null,
                    "Audit log access denied",
                    null
            );
            throw new RuntimeException("Audit log access denied");
        }
    }

    private String clean(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
